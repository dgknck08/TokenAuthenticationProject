package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.JwtUtils;
import com.example.ecommerce.auth.service.JwtBlacklistService;
import com.example.ecommerce.auth.service.JwtValidationService;

@ExtendWith(MockitoExtension.class)
class JwtValidationServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    private JwtValidationService service;

    @BeforeEach
    void setUp() {
        service = new JwtValidationService(jwtTokenProvider, jwtBlacklistService, jwtUtils, userRepository);
    }

    @Test
    void validateToken_throwsWhenTokenBlacklisted() {
        when(jwtBlacklistService.isTokenBlacklisted("token")).thenReturn(true);
        assertThrows(JwtValidationException.class, () -> service.validateToken("token"));
    }

    @Test
    void validateToken_delegatesToProviderWhenNotBlacklisted() {
        when(jwtBlacklistService.isTokenBlacklisted("token")).thenReturn(false);
        when(jwtTokenProvider.validateTokenStructure("token")).thenReturn(true);

        boolean valid = service.validateToken("token");
        assertEquals(true, valid);
        verify(jwtTokenProvider).validateTokenStructure("token");
    }

    @Test
    void getUserIdFromToken_returnsUserIdWhenUsernameExists() {
        User user = User.builder().id(42L).username("alice").build();
        when(jwtUtils.getUsername("token")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Long userId = service.getUserIdFromToken("token");
        assertEquals(42L, userId);
    }

    @Test
    void getUserIdFromToken_throwsWhenUserNotFound() {
        when(jwtUtils.getUsername("token")).thenReturn("missing");
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(JwtValidationException.class, () -> service.getUserIdFromToken("token"));
    }
}
