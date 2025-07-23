package com.example.ecommerce.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.service.JwtBlacklistService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService blacklistService;
    private final String jwtSecret;
    private final int jwtExpirationMs;
    private final String issuer;
    private final SecureRandom secureRandom;

    public JwtTokenProvider(UserDetailsService userDetailsService,
                           JwtBlacklistService blacklistService,
                           @Value("${app.jwtSecret}") String jwtSecret,
                           @Value("${app.jwtExpirationMs}") int jwtExpirationMs,
                           @Value("${app.jwtIssuer:ecommerce-app}") String issuer) {
        this.userDetailsService = userDetailsService;
        this.blacklistService = blacklistService;
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.issuer = issuer;
        this.secureRandom = new SecureRandom();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateTokenWithUsername(String username) {
        return generateTokenWithUsername(username, List.of());
    }

    public String generateTokenWithUsername(String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);
        String tokenId = generateTokenId();
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("token_type", "access");
        claims.put("jti", tokenId); 
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .setNotBefore(Date.from(now))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());
        return generateTokenWithUsername(username, roles);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaimsFromToken(token);
        String username = claims.getSubject();
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        
        List<GrantedAuthority> authorities = roles != null ? 
            roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()) :
            new ArrayList<>();
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getTokenId(String token) {
        return (String) getClaimsFromToken(token).get("jti");
    }

    public boolean validateToken(String token) {
        try {
            // Önce blacklist kontrolü
            if (blacklistService != null && blacklistService.isTokenBlacklisted(token)) {
                throw new JwtValidationException("Token is blacklisted");
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Token tipininin kontrolü
            String tokenType = (String) claims.get("token_type");
            if (tokenType != null && !"access".equals(tokenType)) {
                throw new JwtValidationException("Invalid token type for authentication");
            }
            
            return true;
        } catch (ExpiredJwtException ex) {
            throw new JwtValidationException("JWT token expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException("Invalid JWT token", ex);
        }
    }

    public Instant getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration().toInstant();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        return roles != null ? roles : new ArrayList<>();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String generateTokenId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    //blackliste ekleme
    public void invalidateToken(String token) {
        if (blacklistService != null) {
            blacklistService.blacklistToken(token);
        }
    }

    //tüm tokenler geçersiz
    public void invalidateUserTokens(String username) {
        if (blacklistService != null) {
            blacklistService.blacklistUserTokens(username);
        }
    }
}