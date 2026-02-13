package com.example.ecommerce.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtUtils {

    private static final int MIN_HS512_KEY_BYTES = 64;
    private final SecretKey signingKey;

    public JwtUtils(@Value("${app.jwtSecret}") String jwtSecret) {
        byte[] keyBytes = decodeSecret(jwtSecret);
        if (keyBytes.length < MIN_HS512_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.jwtSecret is too short. Use at least 64 bytes for HS512 (raw bytes, hex, or base64).");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] decodeSecret(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("app.jwtSecret is required and must not be blank.");
        }

        if (isValidHexString(jwtSecret) && jwtSecret.length() % 2 == 0) {
            return hexStringToByteArray(jwtSecret);
        }

        try {
            return Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ignored) {
            return jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static boolean isValidHexString(String s) {
        return s != null && s.matches("[0-9a-fA-F]+");
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Instant getExpirationDate(String token) throws JwtException {
        return parseToken(token).getExpiration().toInstant();
    }

    public String getUsername(String token) throws JwtException {
        return parseToken(token).getSubject();
    }

    public String getTokenId(String token) throws JwtException {
        return (String) parseToken(token).get("jti");
    }

    public boolean isTokenExpired(String token) {
        try {
            return getExpirationDate(token).isBefore(Instant.now());
        } catch (JwtException e) {
            return true;
        }
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}
