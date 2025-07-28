package com.example.ecommerce.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JwtUtils {
    
    private final SecretKey signingKey;
    
    public JwtUtils(@Value("${app.jwtSecret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
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
}