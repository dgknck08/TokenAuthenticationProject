package com.example.ecommerce.auth.service;

import org.springframework.stereotype.Service;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.JwtUtils;

@Service
public class JwtValidationService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtUtils jwtUtils;
    private final UserService userService; // Artık kullanılıyor
    
    public JwtValidationService(JwtTokenProvider jwtTokenProvider, 
                               JwtBlacklistService jwtBlacklistService,
                               JwtUtils jwtUtils,
                               UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    public boolean validateToken(String token) throws JwtValidationException {
        if (jwtBlacklistService.isTokenBlacklisted(token)) {
            throw new JwtValidationException("Token is blacklisted");
        }
        
        return jwtTokenProvider.validateTokenStructure(token);
    }
    
    public void invalidateToken(String token) {
        jwtBlacklistService.blacklistToken(token);
        jwtTokenProvider.invalidateTokenFromCache(token);
    }
    
    public void invalidateUserTokens(String username) {
        jwtBlacklistService.blacklistUserTokens(username);
        jwtTokenProvider.invalidateUserFromCache(username);
    }
    
    public Long getUserIdFromToken(String token) throws JwtValidationException {
        try {
            String username = jwtUtils.getUsername(token);
            return getUserIdByUsername(username);
        } catch (Exception e) {
            throw new JwtValidationException("Failed to extract user ID from token", e);
        }
    }
    
    public String getUsernameFromToken(String token) throws JwtValidationException {
        try {
            return jwtUtils.getUsername(token);
        } catch (Exception e) {
            throw new JwtValidationException("Failed to extract username from token", e);
        }
    }
    
    private Long getUserIdByUsername(String username) throws JwtValidationException {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new JwtValidationException("User not found: " + username));
        return user.getId();
    }
}