package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class AccountLockoutService {

    private static final Logger logger = LoggerFactory.getLogger(AccountLockoutService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditService auditService;

    private static final String FAILED_ATTEMPTS_KEY = "auth:failed_attempts:";
    private static final String ACCOUNT_LOCKED_KEY = "auth:account_locked:";
    private static final String IP_ATTEMPTS_KEY = "auth:ip_attempts:";
    private static final String SUSPICIOUS_LOGIN_KEY = "auth:suspicious:";

    @Value("${app.security.account-lockout.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.account-lockout.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.account-lockout.attempt-window-minutes:15}")
    private int attemptWindowMinutes;

    @Value("${app.security.account-lockout.ip-max-attempts:10}")
    private int ipMaxAttempts;

    public AccountLockoutService(RedisTemplate<String, Object> redisTemplate,
                                 AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.auditService = auditService;
    }

    public void recordLoginAttempt(String username, boolean successful, String failureReason, HttpServletRequest request) {
        recordLoginAttemptAsync(username, successful, failureReason, request);
    }
    /**
     * Kullanıcının giriş denemesini asenkron olarak işler.
     * Başarılıysa bilgileri kaydeder, başarısızsa deneme sayısını artırır.
     * Gerekirse hesabı kilitler ve denetim kaydı oluşturur.
     */
    @Async("authExecutor")
    public CompletableFuture<Void> recordLoginAttemptAsync(String username, boolean successful, String failureReason, HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            if (!successful) {
                Long[] results = handleFailedLoginWithPipeline(username, ipAddress);
                Long userFailedAttempts = results[0];
                Long ipFailedAttempts = results[1];

                saveLoginAttemptDetails(username, ipAddress, userAgent, false, failureReason, userFailedAttempts.intValue());

                if (userFailedAttempts >= maxFailedAttempts || ipFailedAttempts >= ipMaxAttempts) {
                    lockAccount(username, userFailedAttempts.intValue(), request);
                }

                logger.warn("Failed login attempt for user: {} from IP: {}. Attempt count: {}", username, ipAddress, userFailedAttempts);
            } else {
                handleSuccessfulLoginWithPipeline(username, ipAddress, userAgent, request);
                logger.info("Successful login for user: {} from IP: {}", username, ipAddress);
            }

            auditService.logAuthEventAsync(
                null,
                username,
                successful ? AuditLog.AuditAction.USER_LOGIN_SUCCESS : AuditLog.AuditAction.USER_LOGIN_FAILURE,
                successful ? "Login successful" : "Login failed: " + failureReason,
                request
            );

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error recording login attempt", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    /**
     * Başarısız giriş denemelerini Redis'e kaydeder.
     * Kullanıcı ve IP adresi için deneme sayılarını artırır ve sürelerini ayarlar.
     *
     * @param username   Kullanıcı adı
     * @param ipAddress  IP adresi
     * @return Kullanıcı ve IP için güncel deneme sayıları
     */

    private Long[] handleFailedLoginWithPipeline(String username, String ipAddress) {
        String failedAttemptsKey = FAILED_ATTEMPTS_KEY + username;
        String ipAttemptsKey = IP_ATTEMPTS_KEY + ipAddress;

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisStringCommands stringCommands = connection.stringCommands();
            RedisKeyCommands keyCommands = connection.keyCommands();

            byte[] failedKey = failedAttemptsKey.getBytes(StandardCharsets.UTF_8);
            byte[] ipKey = ipAttemptsKey.getBytes(StandardCharsets.UTF_8);

            stringCommands.incr(failedKey);
            keyCommands.expire(failedKey, attemptWindowMinutes * 60);

            stringCommands.incr(ipKey);
            keyCommands.expire(ipKey, attemptWindowMinutes * 60);

            return null;
        });

        return new Long[]{
            (Long) results.get(0),
            (Long) results.get(2)
        };
    }

    private void handleSuccessfulLoginWithPipeline(String username, String ipAddress, String userAgent, HttpServletRequest request) {
        String failedAttemptsKey = FAILED_ATTEMPTS_KEY + username;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisKeyCommands keyCommands = connection.keyCommands();
            keyCommands.del(failedAttemptsKey.getBytes());
            return null;
        });

        checkSuspiciousActivity(username, ipAddress, userAgent);
        saveLoginAttemptDetails(username, ipAddress, userAgent, true, null, 0);
    }

    private void lockAccount(String username, int attemptCount, HttpServletRequest request) {
        String lockedKey = ACCOUNT_LOCKED_KEY + username;

        Map<String, Object> lockInfo = new HashMap<>();
        lockInfo.put("lockedAt", Instant.now().toString());
        lockInfo.put("lockedUntil", Instant.now().plusSeconds(lockoutDurationMinutes * 60).toString());
        lockInfo.put("attemptCount", attemptCount);
        lockInfo.put("reason", "Too many failed login attempts");

        redisTemplate.opsForValue().set(lockedKey, lockInfo, Duration.ofMinutes(lockoutDurationMinutes));

        logger.warn("Account locked for user: {} due to {} failed attempts", username, attemptCount);

        auditService.logAuthEventAsync(
            null,
            username,
            AuditLog.AuditAction.USER_ACCOUNT_LOCKED,
            String.format("Account locked due to %d failed login attempts", attemptCount),
            request
        );
    }

    public boolean isAccountLocked(String username) {
        String lockedKey = ACCOUNT_LOCKED_KEY + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey));
    }

    public Map<String, Object> getAccountLockInfo(String username) {
        String lockedKey = ACCOUNT_LOCKED_KEY + username;
        @SuppressWarnings("unchecked")
        Map<String, Object> lockInfo = (Map<String, Object>) redisTemplate.opsForValue().get(lockedKey);
        return lockInfo;
    }

    public void unlockAccount(String username) {
        String lockedKey = ACCOUNT_LOCKED_KEY + username;
        String failedAttemptsKey = FAILED_ATTEMPTS_KEY + username;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisKeyCommands keyCommands = connection.keyCommands();
            keyCommands.del(lockedKey.getBytes());
            keyCommands.del(failedAttemptsKey.getBytes());
            return null;
        });

        logger.info("Account manually unlocked for user: {}", username);

        auditService.logAuthEventAsync(
            null,
            username,
            AuditLog.AuditAction.USER_ACCOUNT_UNLOCKED,
            "Account manually unlocked by administrator",
            null
        );
    }

    public int getFailedAttemptCount(String username) {
        String failedAttemptsKey = FAILED_ATTEMPTS_KEY + username;
        Object count = redisTemplate.opsForValue().get(failedAttemptsKey);
        return count != null ? ((Number) count).intValue() : 0;
    }

    private void checkSuspiciousActivity(String username, String ipAddress, String userAgent) {
        String suspiciousKey = SUSPICIOUS_LOGIN_KEY + username;

        @SuppressWarnings("unchecked")
        Map<String, Object> lastLogin = (Map<String, Object>) redisTemplate.opsForValue().get(suspiciousKey);

        boolean suspicious = false;
        String reason = "";

        if (lastLogin != null) {
            String lastIp = (String) lastLogin.get("ipAddress");
            String lastUserAgent = (String) lastLogin.get("userAgent");

            if (!ipAddress.equals(lastIp)) {
                suspicious = true;
                reason += "New location. ";
            }

            if (!userAgent.equals(lastUserAgent)) {
                suspicious = true;
                reason += "New device. ";
            }
        }

        Map<String, Object> currentLogin = new HashMap<>();
        currentLogin.put("ipAddress", ipAddress);
        currentLogin.put("userAgent", userAgent);
        currentLogin.put("timestamp", Instant.now().toString());

        redisTemplate.opsForValue().set(suspiciousKey, currentLogin, Duration.ofDays(7));

        if (suspicious) {
            logger.warn("Suspicious login detected for user: {} - {}", username, reason.trim());

            auditService.logAuthEventAsync(
                null,
                username,
                AuditLog.AuditAction.SUSPICIOUS_ACTIVITY,
                String.format("Suspicious login: %s IP: %s", reason.trim(), ipAddress),
                null
            );
        }
    }

    private void saveLoginAttemptDetails(String username, String ipAddress, String userAgent, boolean successful, String failureReason, int attemptCount) {
        try {
            Map<String, Object> attemptDetails = new HashMap<>();
            attemptDetails.put("username", username);
            attemptDetails.put("ipAddress", ipAddress);
            attemptDetails.put("userAgent", userAgent);
            attemptDetails.put("successful", successful);
            attemptDetails.put("failureReason", failureReason);
            attemptDetails.put("attemptCount", attemptCount);
            attemptDetails.put("timestamp", Instant.now().toString());

            String key = "auth:attempt_details:" + username + ":" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(key, attemptDetails, Duration.ofDays(1));
        } catch (Exception e) {
            logger.error("Error saving login attempt details", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
