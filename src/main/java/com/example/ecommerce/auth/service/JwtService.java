package com.example.ecommerce.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.ecommerce.auth.security.JwtTokenProvider;

@Service
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider; // ya da kendi implementasyonun

    public JwtService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String generateToken(String username) {
        return jwtTokenProvider.generateTokenWithUsername(username);
    }

    public String generateToken(Authentication authentication) {
        return jwtTokenProvider.generateToken(authentication);
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }
}
