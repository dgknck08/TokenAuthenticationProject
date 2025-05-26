package com.example.ecommerce.service;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.JwtService;

public class JwtServiceTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        jwtService = new JwtService(jwtTokenProvider);
    }

    @Test
    void generateToken_ByUsername_ShouldReturnToken() {
        String username = "user123";
        String token = "jwt-token";

        when(jwtTokenProvider.generateTokenWithUsername(username)).thenReturn(token);

        String result = jwtService.generateToken(username);

        assertEquals(token, result);
        verify(jwtTokenProvider).generateTokenWithUsername(username);
    }

    @Test
    void generateToken_ByAuthentication_ShouldReturnToken() {
        Authentication auth = mock(Authentication.class);
        String token = "jwt-auth-token";

        when(jwtTokenProvider.generateToken(auth)).thenReturn(token);

        String result = jwtService.generateToken(auth);

        assertEquals(token, result);
        verify(jwtTokenProvider).generateToken(auth);
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenTokenValid() {
        String token = "valid-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);

        boolean result = jwtService.validateToken(token);

        assertTrue(result);
        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenInvalid() {
        String token = "invalid-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        boolean result = jwtService.validateToken(token);

        assertFalse(result);
        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    void getUsernameFromToken_ShouldReturnUsername() {
        String token = "jwt-token";
        String username = "user123";

        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(username);

        String result = jwtService.getUsernameFromToken(token);

        assertEquals(username, result);
        verify(jwtTokenProvider).getUsernameFromToken(token);
    }
}
