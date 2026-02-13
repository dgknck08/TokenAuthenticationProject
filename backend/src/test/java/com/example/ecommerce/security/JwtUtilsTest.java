package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    void constructor_shouldThrow_whenSecretIsBlank() {
        assertThrows(IllegalStateException.class, () -> new JwtUtils(" "));
    }

    @Test
    void constructor_shouldThrow_whenSecretIsTooShort() {
        assertThrows(IllegalStateException.class, () -> new JwtUtils("short-secret"));
    }

    @Test
    void parseToken_shouldWork_withBase64Secret() {
        String rawSecret = "a".repeat(64);
        String base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());
        JwtUtils jwtUtils = new JwtUtils(base64Secret);

        String token = Jwts.builder()
                .setSubject("alice")
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(jwtUtils.getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        assertEquals("alice", jwtUtils.getUsername(token));
        assertTrue(!jwtUtils.isTokenExpired(token));
    }

    @Test
    void parseToken_shouldWork_withHexSecret() {
        String hexSecret = "ab".repeat(64);
        JwtUtils jwtUtils = new JwtUtils(hexSecret);

        String token = Jwts.builder()
                .setSubject("bob")
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(jwtUtils.getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        assertEquals("bob", jwtUtils.getUsername(token));
    }

    @Test
    void isTokenExpired_shouldReturnTrue_forExpiredToken() {
        JwtUtils jwtUtils = new JwtUtils("ab".repeat(64));
        String expiredToken = Jwts.builder()
                .setSubject("old-user")
                .setIssuedAt(Date.from(Instant.now().minusSeconds(120)))
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(jwtUtils.getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        assertTrue(jwtUtils.isTokenExpired(expiredToken));
    }
}
