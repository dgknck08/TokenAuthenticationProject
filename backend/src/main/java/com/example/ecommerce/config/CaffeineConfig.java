package com.example.ecommerce.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.jsonwebtoken.Claims;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfig {

    @Value("${app.cache.jwt.max-size:10000}")
    private int jwtCacheMaxSize;

    @Value("${app.cache.jwt.expire-after-write-minutes:15}")
    private int jwtCacheExpireMinutes;

    @Value("${app.cache.user.max-size:1000}")
    private int userCacheMaxSize;

    @Value("${app.cache.user.expire-after-write-minutes:30}")
    private int userCacheExpireMinutes;

    

    @Bean("jwtValidationCache")
    public Cache<String, Boolean> jwtValidationCache() {
        return Caffeine.newBuilder()
                .maximumSize(jwtCacheMaxSize)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

   
    @Bean("tokenMetadataCache")
    public Cache<String, Object> tokenMetadataCache() {
        return Caffeine.newBuilder()
                .maximumSize(jwtCacheMaxSize)
                .expireAfterWrite(jwtCacheExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    @Bean("jwtClaimsCache")
    public Cache<String, Claims> jwtClaimsCache() {
        return Caffeine.newBuilder()
                .maximumSize(jwtCacheMaxSize)
                .expireAfterWrite(jwtCacheExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean("userDetailsCache")
    public Cache<String, UserDetails> userDetailsCache() {
        return Caffeine.newBuilder()
                .maximumSize(userCacheMaxSize)
                .expireAfterWrite(userCacheExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }


}