package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.model.LoginAttempt;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.LoginAttemptRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class AccountLockoutService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountLockoutService.class);
    
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    
    @Value("${app.security.account-lockout.max-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.security.account-lockout.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    @Value("${app.security.account-lockout.attempt-window-minutes:15}")
    private int attemptWindowMinutes;
    
    public AccountLockoutService(LoginAttemptRepository loginAttemptRepository,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }
    
    public void recordLoginAttempt(String username, boolean successful, String failureReason, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        User user = null; 

        LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .successful(successful)
                .failureReason(failureReason)
                .build();
        
        if (!successful) {
            user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                user.incrementFailedAttempts();
                attempt.setAttemptCount(user.getFailedLoginAttempts());
                
                if (shouldLockAccount(user, ipAddress)) {
                    lockAccount(user, attempt);
                }
                
                userRepository.save(user);
            }
        } else {
            user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                user.resetFailedAttempts();
                userRepository.save(user);
                
                checkSuspiciousActivity(user, ipAddress, userAgent);
            }
        }
        
        loginAttemptRepository.save(attempt);
        
        auditService.logAuthEvent(
            user != null ? user.getId() : null,
            username,
            successful ? AuditLog.AuditAction.USER_LOGIN_SUCCESS : AuditLog.AuditAction.USER_LOGIN_FAILURE,
            successful ? "Login successful" : "Login failed: " + failureReason,
            request
        );
    }
    
    private boolean shouldLockAccount(User user, String ipAddress) {
        if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
            return true;
        }
        
        Instant windowStart = Instant.now().minus(attemptWindowMinutes, ChronoUnit.MINUTES);
        long ipFailedAttempts = loginAttemptRepository.countFailedAttemptsByIpAddress(ipAddress, windowStart);
        
        return ipFailedAttempts >= maxFailedAttempts * 2; 
    }
    
    private void lockAccount(User user, LoginAttempt attempt) {
        Instant lockUntil = Instant.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES);
        user.lockAccount(lockUntil);
        attempt.setAccountLocked(true);
        
        logger.warn("Account locked for user: {} until: {}", user.getUsername(), lockUntil);
        
        auditService.logAuthEvent(
            user.getId(),
            user.getUsername(),
            AuditLog.AuditAction.USER_ACCOUNT_LOCKED,
            String.format("Account locked due to %d failed login attempts. Locked until: %s", 
                         user.getFailedLoginAttempts(), lockUntil),
            null
        );
    }
    
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
                .map(user -> !user.isAccountNonLocked())
                .orElse(false);
    }
    
    public void unlockAccount(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.isAccountLocked()) {
                user.setAccountLocked(false);
                user.setLockedUntil(null);
                user.resetFailedAttempts();
                userRepository.save(user);
                
                logger.info("Account manually unlocked for user: {}", username);
                
                auditService.logAuthEvent(
                    user.getId(),
                    user.getUsername(),
                    AuditLog.AuditAction.USER_ACCOUNT_UNLOCKED,
                    "Account manually unlocked by administrator",
                    null
                );
            }
        });
    }
    
    private void checkSuspiciousActivity(User user, String ipAddress, String userAgent) {
        List<LoginAttempt> recentSuccessfulLogins = loginAttemptRepository.findLastSuccessfulLogins(user.getUsername());
        
        boolean newLocation = recentSuccessfulLogins.stream()
                .noneMatch(attempt -> ipAddress.equals(attempt.getIpAddress()));
        
        boolean newDevice = recentSuccessfulLogins.stream()
                .noneMatch(attempt -> userAgent != null && userAgent.equals(attempt.getUserAgent()));
        
        if (newLocation || newDevice) {
            auditService.logAuthEvent(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.SUSPICIOUS_ACTIVITY,
                String.format("Login from new %s. IP: %s, UserAgent: %s", 
                             newLocation ? "location" : "device", ipAddress, userAgent),
                null
            );
            
            logger.warn("Suspicious login detected for user: {} from IP: {}", user.getUsername(), ipAddress);
        }
    }
    
    //IP adresini almak için kullanilan method
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        //X-Forwarded-For: <client-ip>, <proxy1-ip>, <proxy2-ip>, ...
        //ilk IP client ipsini verir.
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim(); 
        }
        //eğer x-forwarded-for headeri yoksa burasi kullanilacak.
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        //hiç Header yoksa burasi.
        return request.getRemoteAddr();
    }
}
