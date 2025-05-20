package com.example.ecommerce.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAge) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // Lokal test için false, prod’da true yap
        cookie.setPath("/");      // Çok önemli, tüm pathlerde geçerli olsun
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // Silmek için maxAge 0 yap
        response.addCookie(cookie);
    }
}

