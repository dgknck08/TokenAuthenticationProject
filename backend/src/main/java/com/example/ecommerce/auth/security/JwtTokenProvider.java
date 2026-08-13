package com.example.ecommerce.auth.security;

import io.jsonwebtoken.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class JwtTokenProvider {
    private static final String CACHE_STATS_KEY = "stats";
    
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final int jwtExpirationMs;
    private final String issuer;
    private final SecureRandom secureRandom;

    private final Cache<String, Claims> jwtClaimsCache;
    private final Cache<String, Boolean> jwtValidationCache;
    private final Cache<String, UserDetails> userDetailsCache;

    public JwtTokenProvider(UserDetailsService userDetailsService,
            JwtUtils jwtUtils,
            @Qualifier("jwtClaimsCache") Cache<String, Claims> jwtClaimsCache,
            @Qualifier("jwtValidationCache") Cache<String, Boolean> jwtValidationCache,
            @Qualifier("userDetailsCache") Cache<String, UserDetails> userDetailsCache,
            @Value("${app.jwtExpirationMs}") int jwtExpirationMs,
            @Value("${app.jwtIssuer:ecommerce-app}") String issuer) {
this.userDetailsService = userDetailsService;
this.jwtUtils = jwtUtils;
this.jwtExpirationMs = jwtExpirationMs;
this.issuer = issuer;
this.secureRandom = new SecureRandom();

this.jwtClaimsCache = jwtClaimsCache;
this.jwtValidationCache = jwtValidationCache;
this.userDetailsCache = userDetailsCache;
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
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .setNotBefore(Date.from(now))
                .signWith(jwtUtils.getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        try {
            Claims tokenClaims = jwtUtils.parseToken(token);
            jwtClaimsCache.put(cacheKey(token), tokenClaims);
        } catch (Exception e) {
        }
        
        return token;
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(authority -> authority.startsWith("ROLE_"))
                            .toList();
        return generateTokenWithUsername(username, roles);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaimsFromToken(token);
        String username = claims.getSubject();

        UserDetails userDetails = userDetailsCache.get(username, key -> {
            try {
                return CachedUserDetails.from(userDetailsService.loadUserByUsername(key));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load authenticated user details", e);
            }
        });

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getTokenId(String token) {
        return (String) getClaimsFromToken(token).get("jti");
    }

    public boolean validateTokenStructure(String token) {
        String cacheKey = cacheKey(token);
        return jwtValidationCache.get(cacheKey, ignored -> {
            try {
                Claims claims = jwtUtils.parseToken(token);

                String tokenType = (String) claims.get("token_type");
                if (tokenType != null && !"access".equals(tokenType)) {
                    throw new JwtValidationException("Invalid token type for authentication");
                }

                jwtClaimsCache.put(cacheKey, claims);
                
                return true;
            } catch (ExpiredJwtException ex) {
                throw new JwtValidationException("JWT token expired", ex);
            } catch (JwtException | IllegalArgumentException ex) {
                throw new JwtValidationException("Invalid JWT token", ex);
            }
        });
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
        String cacheKey = cacheKey(token);
        Claims cachedClaims = jwtClaimsCache.getIfPresent(cacheKey);
        if (cachedClaims != null) {
            return cachedClaims;
        }
        
        return jwtClaimsCache.get(cacheKey, ignored -> jwtUtils.parseToken(token));
    }

    private String generateTokenId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public void invalidateTokenFromCache(String token) {
        String cacheKey = cacheKey(token);
        jwtClaimsCache.invalidate(cacheKey);
        jwtValidationCache.invalidate(cacheKey);
    }

    public void invalidateUserFromCache(String username) {
        userDetailsCache.invalidate(username);
    }

    public void invalidateAllCaches() {
        jwtClaimsCache.invalidateAll();
        jwtValidationCache.invalidateAll();
        userDetailsCache.invalidateAll();
    }
    
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("jwtClaimsCache", Map.of(
            "size", jwtClaimsCache.estimatedSize(),
            CACHE_STATS_KEY, jwtClaimsCache.stats()
        ));
        stats.put("jwtValidationCache", Map.of(
            "size", jwtValidationCache.estimatedSize(),
            CACHE_STATS_KEY, jwtValidationCache.stats()
        ));
        stats.put("userDetailsCache", Map.of(
            "size", userDetailsCache.estimatedSize(),
            CACHE_STATS_KEY, userDetailsCache.stats()
        ));
        return stats;
    }

    private String cacheKey(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        return DigestUtils.sha256Hex(token);
    }
}
