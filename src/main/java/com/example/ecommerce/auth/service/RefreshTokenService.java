package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.model.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    String validateRefreshToken(String token);
    void deleteByUserId(Long userId);
    void deleteExpiredTokens();
}