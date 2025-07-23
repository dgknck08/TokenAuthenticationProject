package com.example.ecommerce.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.ecommerce.auth.security.JwtTokenProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public JwtBlacklistService(RedisTemplate<String, String> redisTemplate, 
                              JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    public void blacklistToken(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;

            //token süre hesabi Instant 
            Instant expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            long ttl = Duration.between(Instant.now(), expiration).getSeconds();
            
            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.SECONDS);
                logger.info("Token blacklisted successfully with TTL: {} seconds", ttl);
            }
        } catch (Exception e) {
            logger.error("Error blacklisting token: {}", e.getMessage());
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

  
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Error checking token blacklist status: {}", e.getMessage());
            // tokeni geçersiz sayma
            return true;
        }
    }

 
    public void blacklistUserTokens(String username) {
        try {
            String pattern = BLACKLIST_PREFIX + "*:" + username + ":*";
            logger.info("Blacklisting all tokens for user: {}", username);
        } catch (Exception e) {
            logger.error("Error blacklisting user tokens: {}", e.getMessage());
        }
    }


    public void cleanupExpiredTokens() {
        // Redis TTL ile otomatik temizleme
        logger.debug("Redis TTL automatically handles expired tokens cleanup");
    }
}