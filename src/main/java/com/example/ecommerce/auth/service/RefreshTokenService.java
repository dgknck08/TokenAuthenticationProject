package com.example.ecommerce.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerce.auth.exception.TokenRefreshException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final Long refreshTokenDurationMs;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    public RefreshTokenService(
            @Value("${app.jwtRefreshExpirationMs}") Long refreshTokenDurationMs,
            RefreshTokenRepository refreshTokenRepository,
            UserService userService) {
        this.refreshTokenDurationMs = refreshTokenDurationMs;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    public RefreshToken createRefreshToken(Long userId) {
        var user = userService.getUserById(userId);
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser(user);

        if (existingTokenOpt.isPresent()) {
            RefreshToken existingToken = existingTokenOpt.get();
            if (existingToken.getExpiryDate().isAfter(Instant.now())) {
                // Geçerli token varsa onu dön
                return existingToken;
            } else {
                // Süresi dolmuş token varsa sil
                refreshTokenRepository.delete(existingToken);
            }
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }
    
    

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token expired. Please login again.");
        }
        return token;
    }

    public String validateRefreshToken(String tokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
            .orElseThrow(() -> new TokenRefreshException(tokenStr, "Refresh token not found"));

        verifyExpiration(refreshToken);
        return refreshToken.getUser().getUsername();
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUser(userService.getUserById(userId));
    }
}
