package com.example.ecommerce.auth.service.impl;

import com.example.ecommerce.auth.exception.TokenRefreshException;

import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final long refreshTokenDurationMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                   UserService userService,
                                   @Value("${app.jwtRefreshExpirationMs:604800000}") long refreshTokenDurationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    
    //register durumlarinda.
    @Override
    public RefreshToken createRefreshToken(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        refreshTokenRepository.deleteByUserId(userId);
        
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                user,
                Instant.now().plusMillis(refreshTokenDurationMs)
        );
        
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        logger.info("Created new refresh token for user: {}", user.getUsername());
        
        return saved;
    }

    
    //refresh token expired durumlarinda.
    @Override
    public String validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));
        
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token was expired. Please make a new signin request");
        }
        
        return refreshToken.getUser().getUsername();
    }

    @Override
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Scheduled(fixedRate = 3600000) 
    @Override
    public void deleteExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired refresh tokens", deletedCount);
        }
    }
}