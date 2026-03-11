package com.example.ecommerce.auth.service.impl;

import com.example.ecommerce.auth.exception.EmailVerificationException;
import com.example.ecommerce.auth.model.EmailVerificationToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.EmailVerificationTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskExecutor;
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
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final Optional<JavaMailSender> mailSender;
    private final TaskExecutor authExecutor;
    private final String emailFrom;
    private final String verificationBaseUrl;
    private final long verificationExpirationMs;

    public EmailVerificationServiceImpl(EmailVerificationTokenRepository tokenRepository,
                                        UserRepository userRepository,
                                        Optional<JavaMailSender> mailSender,
                                        @Qualifier("authExecutor") TaskExecutor authExecutor,
                                        @Value("${app.mail.from:no-reply@ecommerce.local}") String emailFrom,
                                        @Value("${app.email.verification-base-url:http://localhost:3000/verify-email}") String verificationBaseUrl,
                                        @Value("${app.email.verification-expiration-ms:1800000}") long verificationExpirationMs) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.authExecutor = authExecutor;
        this.emailFrom = emailFrom;
        this.verificationBaseUrl = verificationBaseUrl;
        this.verificationExpirationMs = verificationExpirationMs;
    }

    @Override
    public void createAndSendVerification(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required for verification");
        }
        if (user.isEmailVerified()) {
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateToken();
        EmailVerificationToken tokenEntity = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusMillis(verificationExpirationMs))
                .build();
        tokenRepository.save(tokenEntity);

        String verificationUrl = buildFrontendTokenUrl(verificationBaseUrl, rawToken);
        authExecutor.execute(() -> sendVerificationEmail(user.getEmail(), user.getFullName(), verificationUrl));
        logger.info("Email verification token created for user: {}", sanitizeForLog(user.getUsername()));
    }

    @Override
    public void verifyToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new EmailVerificationException("Verification token is required");
        }

        String tokenHash = hashToken(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new EmailVerificationException("Invalid verification token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new EmailVerificationException("Verification token has expired");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
        logger.info("Email verified successfully for user: {}", sanitizeForLog(user.getUsername()));
    }

    @Override
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                createAndSendVerification(user);
            }
        });
    }

    private void sendVerificationEmail(String to, String fullName, String verificationUrl) {
        if (to == null || to.isBlank()) {
            return;
        }

        if (mailSender.isEmpty()) {
            logger.warn("JavaMailSender is not configured. Verification email was not sent for {}.", sanitizeForLog(to));
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(to);
            message.setSubject("Verify your email");
            message.setText(buildEmailBody(fullName, verificationUrl));
            mailSender.get().send(message);
            logger.info("Verification email sent to {}", sanitizeForLog(to));
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", sanitizeForLog(to), e);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpiredOrUsed(Instant.now());
            if (deletedCount > 0) {
                logger.info("Cleaned up {} email verification token(s)", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup email verification tokens", e);
        }
    }

    private String buildEmailBody(String fullName, String verificationUrl) {
        String displayName = (fullName == null || fullName.isBlank()) ? "there" : fullName;
        return "Hi " + displayName + ",\n\n"
                + "Please verify your email by opening the link below:\n"
                + verificationUrl + "\n\n"
                + "If you did not create an account, you can ignore this email.";
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
