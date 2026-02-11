package com.example.ecommerce.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;

public class CookieUtil {

    private static final String COOKIE_NAME = "refreshToken";
    private static final boolean COOKIE_SECURE =
            Boolean.parseBoolean(System.getenv().getOrDefault("APP_COOKIE_SECURE", "true"));

    public static ResponseCookie createRefreshTokenCookie(String token, int maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build();
    }

    public static ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public static String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }
}
