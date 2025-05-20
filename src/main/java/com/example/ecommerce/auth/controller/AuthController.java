package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.*;
import com.example.ecommerce.auth.exception.TokenRefreshException;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        LoginResponse authResponse = authService.login(loginRequest);

        // Refresh token'ı cookie ile gönderiyoruz.
        CookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 7 * 24 * 60 * 60);

        // Access token'ı JSON olarak dönüyoruz, refresh token cookie'de.
        LoginResponse responseBody = new LoginResponse(
            authResponse.getAccessToken(),
            null,
            authResponse.getUsername(),
            authResponse.getEmail()
        );

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        RegisterResponse authResponse = authService.register(registerRequest);

        CookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 7 * 24 * 60 * 60);

        RegisterResponse responseBody = new RegisterResponse(
            authResponse.getAccessToken(),
            null,
            authResponse.getUsername(),
            authResponse.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);
            RefreshTokenResponse authResponse = authService.refreshToken(refreshToken);

            CookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 7 * 24 * 60 * 60);

            RefreshTokenResponse responseBody = new RefreshTokenResponse(
                authResponse.getAccessToken(),
                null,
                authResponse.getUsername(),
                authResponse.getEmail() 
            );

            return ResponseEntity.ok(responseBody);
        } catch (TokenRefreshException e) {
            CookieUtil.deleteRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RefreshTokenResponse(null, null, "Invalid or expired refresh token. Please login again."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        CookieUtil.deleteRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
