package com.example.ecommerce.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAge) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);  // Prod ortamda https kullanıyorsan true yap
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        // SameSite özelliği cookie API'sinde olmadığı için header ile ekliyoruz
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(REFRESH_TOKEN_COOKIE_NAME).append("=").append(refreshToken)
                .append("; Max-Age=").append(maxAge)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; Secure")
                .append("; SameSite=Strict");

        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        // Aynı şekilde SameSite header olarak ekle
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(REFRESH_TOKEN_COOKIE_NAME).append("=")
                .append("; Max-Age=0")
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; Secure")
                .append("; SameSite=Strict");

        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

    public static void addAccessTokenCookie(HttpServletResponse response, String accessToken, int maxAge) {
        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(false); // Access token frontend JS tarafından okunacaksa false olmalı
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append("accessToken").append("=").append(accessToken)
                .append("; Max-Age=").append(maxAge)
                .append("; Path=/")
                .append("; Secure")
                .append("; SameSite=Lax");

        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

}
