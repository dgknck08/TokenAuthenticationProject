package com.example.ecommerce.service;

import com.example.ecommerce.auth.exception.EmailVerificationException;
import com.example.ecommerce.auth.model.EmailVerificationToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.EmailVerificationTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.impl.EmailVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;

    private final TaskExecutor syncExecutor = Runnable::run;

    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = build(Optional.of(mailSender));
    }

    private EmailVerificationServiceImpl build(Optional<JavaMailSender> sender) {
        return new EmailVerificationServiceImpl(
                tokenRepository, userRepository, sender, syncExecutor,
                "no-reply@test.local", "http://localhost:3000/verify-email", 1_800_000L);
    }

    private User user(Long id, boolean verified) {
        return User.builder()
                .id(id)
                .username("alice")
                .email("alice@test.local")
                .emailVerified(verified)
                .build();
    }

    @Test
    void createAndSendVerification_shouldRejectNullUser() {
        assertThrows(IllegalArgumentException.class, () -> service.createAndSendVerification(null));
    }

    @Test
    void createAndSendVerification_shouldRejectUserWithoutId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAndSendVerification(user(null, false)));
    }

    @Test
    void createAndSendVerification_shouldSkipWhenAlreadyVerified() {
        service.createAndSendVerification(user(1L, true));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void createAndSendVerification_shouldPersistTokenAndSendEmail() {
        service.createAndSendVerification(user(1L, false));

        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void createAndSendVerification_shouldNotFailWhenMailSenderMissing() {
        EmailVerificationServiceImpl noMail = build(Optional.empty());
        noMail.createAndSendVerification(user(1L, false));
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    void createAndSendVerification_shouldSwallowMailSendErrors() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        service.createAndSendVerification(user(1L, false));
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    // ── verifyToken ───────────────────────────────────────────
    @Test
    void verifyToken_shouldRejectBlankToken() {
        assertThrows(EmailVerificationException.class, () -> service.verifyToken("  "));
    }

    @Test
    void verifyToken_shouldRejectUnknownToken() {
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.empty());
        assertThrows(EmailVerificationException.class, () -> service.verifyToken("raw-token"));
    }

    @Test
    void verifyToken_shouldRejectExpiredToken() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user(1L, false))
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));
        assertThrows(EmailVerificationException.class, () -> service.verifyToken("raw-token"));
    }

    @Test
    void verifyToken_shouldMarkUserVerified() {
        User user = user(1L, false);
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));

        service.verifyToken("raw-token");

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(tokenRepository).deleteByUserId(1L);
    }

    @Test
    void resendVerification_shouldSkipBlankEmail() {
        service.resendVerification("   ");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resendVerification_shouldResendForUnverifiedUser() {
        when(userRepository.findByEmail("alice@test.local")).thenReturn(Optional.of(user(1L, false)));
        service.resendVerification("Alice@Test.Local");
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    void resendVerification_shouldSkipVerifiedUser() {
        when(userRepository.findByEmail("alice@test.local")).thenReturn(Optional.of(user(1L, true)));
        service.resendVerification("alice@test.local");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resendVerification_shouldSkipUnknownEmail() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());
        service.resendVerification("ghost@test.local");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void cleanupExpiredTokens_shouldDeleteExpired() {
        when(tokenRepository.deleteExpiredOrUsed(any())).thenReturn(4);
        service.cleanupExpiredTokens();
        verify(tokenRepository).deleteExpiredOrUsed(any());
    }

    @Test
    void cleanupExpiredTokens_shouldSwallowErrors() {
        when(tokenRepository.deleteExpiredOrUsed(any())).thenThrow(new RuntimeException("db down"));
        service.cleanupExpiredTokens();
        verify(tokenRepository).deleteExpiredOrUsed(any());
    }
}
