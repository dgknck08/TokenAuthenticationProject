package com.example.ecommerce.auth.service.impl;

import com.example.ecommerce.auth.exception.PasswordResetException;
import com.example.ecommerce.auth.model.PasswordResetToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.PasswordResetTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Optional<JavaMailSender> mailSender;
    private final TaskExecutor authExecutor;
    private final String emailFrom;
    private final String passwordResetBaseUrl;
    private final long passwordResetExpirationMs;

    public PasswordResetServiceImpl(PasswordResetTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    Optional<JavaMailSender> mailSender,
                                    @Qualifier("authExecutor") TaskExecutor authExecutor,
                                    @Value("${app.mail.from:no-reply@ecommerce.local}") String emailFrom,
                                    @Value("${app.password-reset.base-url:http://localhost:3000/auth/reset-password}") String passwordResetBaseUrl,
                                    @Value("${app.password-reset.expiration-ms:900000}") long passwordResetExpirationMs) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.authExecutor = authExecutor;
        this.emailFrom = emailFrom;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
        this.passwordResetExpirationMs = passwordResetExpirationMs;
    }

    @Override
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(normalizedEmail).ifPresent(this::createAndSendToken);
    }

    @Override
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new PasswordResetException("Reset token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new PasswordResetException("New password is required");
        }

        String tokenHash = hashToken(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new PasswordResetException("Invalid password reset token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new PasswordResetException("Password reset token has expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
        logger.info("Password reset completed for user: {}", sanitizeForLog(user.getUsername()));
    }

    private void createAndSendToken(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateToken();
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusMillis(passwordResetExpirationMs))
                .build();
        tokenRepository.save(tokenEntity);

        String resetUrl = buildFrontendTokenUrl(passwordResetBaseUrl, rawToken);
        authExecutor.execute(() -> sendResetEmail(user.getEmail(), user.getFullName(), resetUrl));
        logger.info("Password reset token created for user: {}", sanitizeForLog(user.getUsername()));
    }

    private void sendResetEmail(String to, String fullName, String resetUrl) {
        if (to == null || to.isBlank()) {
            return;
        }

        if (mailSender.isEmpty()) {
            logger.warn("JavaMailSender is not configured. Password reset email was not sent for {}.", sanitizeForLog(to));
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(to);
            message.setSubject("Reset your password");
            message.setText(buildEmailBody(fullName, resetUrl));
            mailSender.get().send(message);
            logger.info("Password reset email sent to {}", sanitizeForLog(to));
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", sanitizeForLog(to), e);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpiredOrUsed(Instant.now());
            if (deletedCount > 0) {
                logger.info("Cleaned up {} password reset token(s)", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup password reset tokens", e);
        }
    }

    private String buildEmailBody(String fullName, String resetUrl) {
        String displayName = (fullName == null || fullName.isBlank()) ? "there" : fullName;
        return "Hi " + displayName + ",\n\n"
                + "Use the link below to reset your password:\n"
                + resetUrl + "\n\n"
                + "If you did not request a password reset, you can ignore this email.";
    }

    private String buildFrontendTokenUrl(String baseUrl, String rawToken) {
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBase.isEmpty()) {
            return "";
        }
        String separator = normalizedBase.contains("#") ? "&" : "#";
        return normalizedBase + separator + "token=" + rawToken;
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\n\\r\\t]", "_");
    }
}
