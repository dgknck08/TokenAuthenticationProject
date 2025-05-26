package com.example.ecommerce.service;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import com.example.ecommerce.auth.exception.TokenRefreshException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RefreshTokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserService userService;
    private RefreshTokenService refreshTokenService;

    private final Long tokenDurationMs = 1000L * 60 * 60; // 1 saat

    @BeforeEach
    void setup() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userService = mock(UserService.class);

        refreshTokenService = new RefreshTokenService(tokenDurationMs, refreshTokenRepository, userService);
    }

    @Test
    void createRefreshToken_ShouldReturnExistingValidToken() {
        User user = new User();
        user.setId(1L);

        RefreshToken existingToken = new RefreshToken();
        existingToken.setUser(user);
        existingToken.setExpiryDate(Instant.now().plusSeconds(300)); // henüz geçerli
        existingToken.setToken("existing-token");

        when(userService.getUserById(1L)).thenReturn(user);
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existingToken));

        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        assertEquals(existingToken.getToken(), result.getToken());
        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void createRefreshToken_ShouldDeleteExpiredTokenAndCreateNewOne() {
        User user = new User();
        user.setId(1L);

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(user);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(300)); // geçmiş
        expiredToken.setToken("expired-token");

        when(userService.getUserById(1L)).thenReturn(user);
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(expiredToken));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        assertNotEquals(expiredToken.getToken(), result.getToken());
        verify(refreshTokenRepository).delete(expiredToken);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void verifyExpiration_ShouldThrowException_WhenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().minusSeconds(1));
        token.setToken("expired-token");

        doNothing().when(refreshTokenRepository).delete(token);

        TokenRefreshException exception = assertThrows(TokenRefreshException.class,
            () -> refreshTokenService.verifyExpiration(token));

        assertEquals("Refresh token expired. Please login again.", exception.getMessage());
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void verifyExpiration_ShouldReturnToken_WhenValid() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().plusSeconds(300));
        token.setToken("valid-token");

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertEquals(token, result);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void validateRefreshToken_ShouldReturnUsername_WhenTokenValid() {
        User user = new User();
        user.setUsername("testuser");

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(300));
        token.setToken("valid-token");

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        String username = refreshTokenService.validateRefreshToken("valid-token");

        assertEquals("testuser", username);
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        TokenRefreshException exception = assertThrows(TokenRefreshException.class,
            () -> refreshTokenService.validateRefreshToken("invalid-token"));

        assertEquals("Refresh token not found", exception.getMessage());
    }

    @Test
    void deleteByUserId_ShouldCallRepositoryDelete() {
        User user = new User();
        user.setId(1L);

        when(userService.getUserById(1L)).thenReturn(user);
        when(refreshTokenRepository.deleteByUser(user)).thenReturn(1);

        int deletedCount = refreshTokenService.deleteByUserId(1L);

        assertEquals(1, deletedCount);
        verify(refreshTokenRepository).deleteByUser(user);
    }
}
