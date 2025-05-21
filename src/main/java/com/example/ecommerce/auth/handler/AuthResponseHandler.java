package com.example.ecommerce.auth.handler;

import com.example.ecommerce.auth.dto.*;
import com.example.ecommerce.util.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

public class AuthResponseHandler {

    private static final int REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60; // 7 gün

    public static ResponseEntity<LoginResponse> handleLogin(LoginResponse response) {
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(response.getRefreshToken(), REFRESH_TOKEN_EXPIRATION);

        LoginResponse responseBody = new LoginResponse(
                response.getAccessToken(),
                null, // Refresh token JSON'da dönülmüyor
                response.getUsername(),
                response.getEmail()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(responseBody);
    }

    public static ResponseEntity<RegisterResponse> handleRegister(RegisterResponse response) {
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(response.getRefreshToken(), REFRESH_TOKEN_EXPIRATION);

        RegisterResponse responseBody = new RegisterResponse(
                response.getAccessToken(),
                null,
                response.getUsername(),
                response.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(responseBody);
    }

    public static ResponseEntity<RefreshTokenResponse> handleRefreshTokenResponse(RefreshTokenResponse response) {
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(response.getRefreshToken(), REFRESH_TOKEN_EXPIRATION);

        RefreshTokenResponse responseBody = new RefreshTokenResponse(
                response.getAccessToken(),
                null,
                response.getUsername(),
                response.getEmail()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(responseBody);
    }

    public static ResponseEntity<Void> handleLogout() {
        ResponseCookie deleteCookie = CookieUtil.deleteRefreshTokenCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }
}
