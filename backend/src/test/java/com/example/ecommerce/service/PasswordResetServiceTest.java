package com.example.ecommerce.service;

import com.example.ecommerce.auth.exception.PasswordResetException;
import com.example.ecommerce.auth.model.PasswordResetToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.PasswordResetTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.impl.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetServiceImpl service;

    @BeforeEach
    void setUp() {
        TaskExecutor directExecutor = Runnable::run;
        service = new PasswordResetServiceImpl(
                tokenRepository,
                userRepository,
                passwordEncoder,
                Optional.empty(),
                directExecutor,
                "no-reply@test.com",
                "http://localhost:3000/auth/reset-password",
                900000
        );
    }

    @Test
    void requestReset_shouldCreateToken_whenUserExists() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("A")
                .password("old")
                .build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        service.requestReset("alice@example.com");

        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
        User user = User.builder()
                .id(11L)
                .username("bob")
                .email("bob@example.com")
                .firstName("Bob")
                .lastName("B")
                .password("old-hash")
                .build();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(99L)
                .user(user)
                .tokenHash("hashed-token")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("new-hash");

        service.resetPassword("raw-token", "NewPass123!");

        verify(userRepository).save(user);
        verify(tokenRepository).deleteByUserId(11L);
    }

    @Test
    void resetPassword_shouldThrow_whenTokenIsMissing() {
        assertThrows(PasswordResetException.class, () -> service.resetPassword("", "NewPass123!"));
    }
}
