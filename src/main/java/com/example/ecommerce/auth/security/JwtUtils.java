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

    private final SecretKey signingKey;

    public JwtUtils(
            @Value("${app.jwtSecret}") String jwtSecret,
            @Value("${spring.profiles.active:default}") String activeProfile
    ) {
        byte[] keyBytes;

        if ("test".equals(activeProfile)) {
            // Test ortamında Base64 decode
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } else if (isValidHexString(jwtSecret) && jwtSecret.length() >= 128) {
            // Hex string ve en az 512 bit (128 hex karakter)
            keyBytes = hexStringToByteArray(jwtSecret);
        } else {
            // Diğer durumlarda Base64 encode edilmemiş string ise UTF-8 ve padding ile 512 bit yap
            keyBytes = padTo512Bits(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
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

    /**
     * Key uzunluğunu 512 bit (64 byte) yapacak şekilde padding ekler.
     */
    private static byte[] padTo512Bits(byte[] original) {
        if (original.length >= 64) return original; // zaten yeterli
        byte[] padded = new byte[64];
        System.arraycopy(original, 0, padded, 0, original.length);
        for (int i = original.length; i < 64; i++) {
            padded[i] = 0; // kalan byte’ları 0 ile doldur
        }
        return padded;
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
