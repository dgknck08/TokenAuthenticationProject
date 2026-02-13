package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.*;

import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.handler.AuthResponseHandler;
import com.example.ecommerce.auth.service.AccountLockoutService;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.JwtBlacklistService;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccountLockoutService accountLockoutService;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtValidationService jwtValidationService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public AuthController(AuthService authService,
                          JwtTokenProvider jwtTokenProvider,
                          AccountLockoutService accountLockoutService,
                          JwtBlacklistService jwtBlacklistService,
                          JwtValidationService jwtValidationService,
                          RefreshTokenService refreshTokenService,
                          UserService userService) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accountLockoutService = accountLockoutService;
        this.jwtBlacklistService = jwtBlacklistService;
        this.jwtValidationService = jwtValidationService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.username();

        if (accountLockoutService.isAccountLocked(username)) {
            Map<String, Object> lockInfo = accountLockoutService.getAccountLockInfo(username);
            String message = "Account is locked. Please try again later.";
            if (lockInfo != null && lockInfo.containsKey("lockedUntil")) {
                message += " Locked until: " + lockInfo.get("lockedUntil");
            }
            return ResponseEntity.status(HttpStatus.LOCKED).body(new LoginErrorResponse(message));
        }

        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            accountLockoutService.recordLoginAttempt(username, true, null, request);

            String token = loginResponse.accessToken();
            if (token != null) {
                String ipAddress = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                jwtBlacklistService.storeTokenMetadata(token, username, ipAddress, userAgent);
            }

            return AuthResponseHandler.handleLogin(loginResponse);

        } catch (InvalidCredentialsException ex) {
            accountLockoutService.recordLoginAttempt(username, false, ex.getMessage(), request);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginErrorResponse("Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return AuthResponseHandler.handleRegister(registerResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);
        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(refreshToken);
        return AuthResponseHandler.handleRefreshTokenResponse(refreshTokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);
        if (token != null) {
            jwtBlacklistService.blacklistToken(token);
            try {
                Long userId = jwtValidationService.getUserIdFromToken(token);
                refreshTokenService.deleteByUserId(userId);
            } catch (Exception ignored) {
                // Token geçersizse refresh token silme atlanır
            }
        }
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        return AuthResponseHandler.handleLogout();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            jwtBlacklistService.blacklistUserTokens(auth.getName());
            try {
                userService.findByUsername(auth.getName())
                    .ifPresent(user -> refreshTokenService.deleteByUserId(user.getId()));
            } catch (Exception ignored) {
                // Kullanıcı id bulunamazsa refresh token silme atlanır
            }
        }
        return AuthResponseHandler.handleLogout(); // refresh tokenı da sil
    }
    @PostMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            try {
                boolean isValid = jwtValidationService.validateToken(token);
                String username = jwtTokenProvider.getUsernameFromToken(token);
                Map<String, Object> tokenMetadata = jwtBlacklistService.getTokenMetadata(token);

                Map<String, Object> response = Map.of(
                        "valid", isValid,
                        "username", username,
                        "roles", jwtTokenProvider.getRolesFromToken(token),
                        "metadata", tokenMetadata != null ? tokenMetadata : Map.of()
                );

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "No token provided"));
    }

    @GetMapping("/account-status/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAccountStatus(@PathVariable String username) {
        boolean isLocked = accountLockoutService.isAccountLocked(username);
        int failedAttempts = accountLockoutService.getFailedAttemptCount(username);
        Map<String, Object> lockInfo = accountLockoutService.getAccountLockInfo(username);

        Map<String, Object> status = Map.of(
                "username", username,
                "locked", isLocked,
                "failedAttempts", failedAttempts,
                "lockInfo", lockInfo != null ? lockInfo : Map.of()
        );

        return ResponseEntity.ok(status);
    }

    @PostMapping("/unlock-account/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> unlockAccount(@PathVariable String username) {
        accountLockoutService.unlockAccount(username);
        return ResponseEntity.ok(Map.of("message", "Account unlocked successfully"));
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
	
}
