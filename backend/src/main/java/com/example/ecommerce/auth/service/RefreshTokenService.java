package com.example.ecommerce.auth.service;

public interface RefreshTokenService {
    String createRefreshToken(Long userId);
    String validateRefreshToken(String token);
    void deleteByUserId(Long userId);
    void deleteByToken(String token);
    void deleteExpiredTokens();
}
