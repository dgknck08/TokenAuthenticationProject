package com.example.ecommerce.service;


import com.example.ecommerce.auth.exception.TokenRefreshException;
import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.auth.service.impl.RefreshTokenServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    private RefreshTokenServiceImpl refreshTokenService;

    private final long refreshTokenDurationMs = 3600000L; // 1 saat

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, userService, refreshTokenDurationMs);
    }

    @Test
    void createRefreshToken_ShouldCreateNewTokenSuccessfully() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(userId);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        verify(refreshTokenRepository).deleteByUserId(userId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_ShouldThrowException_WhenUserNotFound() {

        Long userId = 99L;
        when(userService.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> refreshTokenService.createRefreshToken(userId));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void validateRefreshToken_ShouldReturnUsername_WhenTokenIsValid() {

        User user = new User();
        user.setUsername("validUser");

        RefreshToken token = new RefreshToken();
        token.setTokenHash("hashed-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        String result = refreshTokenService.validateRefreshToken("abc123");

        assertEquals("validUser", result);
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenNotFound() {

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(TokenRefreshException.class, () -> refreshTokenService.validateRefreshToken("invalid"));
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIsExpired() {

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setTokenHash("hashed-expired-token");
        expiredToken.setExpiryDate(Instant.now().minusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(TokenRefreshException.class, () -> refreshTokenService.validateRefreshToken("expired"));
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void deleteByUserId_ShouldDeleteSuccessfully() {

        refreshTokenService.deleteByUserId(5L);

        verify(refreshTokenRepository).deleteByUserId(5L);
    }

    @Test
    void deleteExpiredTokens_ShouldLogWhenTokensDeleted() {

        when(refreshTokenRepository.deleteByExpiryDateBefore(any())).thenReturn(3);

        refreshTokenService.deleteExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiryDateBefore(any());
    }

    @Test
    void deleteExpiredTokens_ShouldNotLog_WhenNoTokensDeleted() {

        when(refreshTokenRepository.deleteByExpiryDateBefore(any())).thenReturn(0);

        refreshTokenService.deleteExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiryDateBefore(any());
    }
}
