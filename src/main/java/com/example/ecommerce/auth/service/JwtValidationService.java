package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class JwtValidationService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    
    public JwtValidationService(JwtTokenProvider jwtTokenProvider, 
                               JwtBlacklistService jwtBlacklistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
    }
    
    public boolean validateToken(String token) throws JwtValidationException {
        //önce blacklist kontrolü
        if (jwtBlacklistService.isTokenBlacklisted(token)) {
            throw new JwtValidationException("Token is blacklisted");
        }
        
        //sonra token yapısı kontrolü
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
}