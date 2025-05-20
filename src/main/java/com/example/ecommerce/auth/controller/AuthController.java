package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.*;
import com.example.ecommerce.auth.exception.TokenRefreshException;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.util.CookieUtil;
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
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(loginRequest);

        // Refresh token'ı cookie ile gönderiyoruz.
        CookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 7 * 24 * 60 * 60);

        // Access token sadece JSON olarak dönülecek.
        AuthResponse responseBody = new AuthResponse(authResponse.getAccessToken(), null, "Bearer");
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(registerRequest);

        CookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 7 * 24 * 60 * 60);

        AuthResponse responseBody = new AuthResponse(authResponse.getAccessToken(), null, "Bearer");
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.refreshToken(request);

            // Yeni refresh token cookie'ye ekle
            CookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 7 * 24 * 60 * 60);

            // Access token JSON olarak dön
            AuthResponse responseBody = new AuthResponse(authResponse.getAccessToken(), null, "Bearer");
            return ResponseEntity.ok(responseBody);
        } catch (TokenRefreshException e) {
            CookieUtil.deleteRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid or expired refresh token. Please login again."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        CookieUtil.deleteRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
