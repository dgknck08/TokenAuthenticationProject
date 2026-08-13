package com.example.ecommerce.service;
import java.util.Arrays;
import java.time.Duration;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.service.AccountLockoutService;
import com.example.ecommerce.auth.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountLockoutServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private AuditService auditService;
    @Mock
    private AccountLockoutService selfProxy;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private RedisConnection connection;
    @Mock
    private RedisStringCommands stringCommands;
    @Mock
    private RedisKeyCommands keyCommands;
    @Mock
    private HttpServletRequest request;

    private AccountLockoutService service;

    @BeforeEach
    void setUp() {
        service = new AccountLockoutService(redisTemplate, auditService, selfProxy);
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(service, "lockoutDurationMinutes", 30);
        ReflectionTestUtils.setField(service, "attemptWindowMinutes", 15);
        ReflectionTestUtils.setField(service, "ipMaxAttempts", 10);
        when(connection.stringCommands()).thenReturn(stringCommands);
        when(connection.keyCommands()).thenReturn(keyCommands);
    }

    @SuppressWarnings({"rawtypes"})
    private void pipelineReturns(List<Object> results) {
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(inv -> {
            ((RedisCallback) inv.getArgument(0)).doInRedis(connection);
            return results;
        });
    }

    @Test
    void recordLoginAttempt_shouldDelegateToAsyncSelfProxy() {
        service.recordLoginAttempt("alice", false, "bad password", request);
        verify(selfProxy).recordLoginAttemptAsync("alice", false, "bad password", request);
    }

    @Test
    void recordLoginAttemptAsync_failedBelowThreshold_shouldNotLock() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 1.1.1.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit-UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pipelineReturns(List.of(2L, Boolean.TRUE, 1L, Boolean.TRUE));

        CompletableFuture<Void> result =
                service.recordLoginAttemptAsync("alice", false, "bad password", request);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.USER_LOGIN_FAILURE), anyString(), eq(request));
    }

    @Test
    void recordLoginAttemptAsync_failedAtThreshold_shouldLockAccount() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("8.8.8.8");
        when(request.getHeader("User-Agent")).thenReturn("JUnit-UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pipelineReturns(List.of(5L, Boolean.TRUE, 1L, Boolean.TRUE));

        service.recordLoginAttemptAsync("alice", false, "bad password", request);

        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.USER_ACCOUNT_LOCKED), anyString(), eq(request));
    }

    @Test
    void recordLoginAttemptAsync_ipThresholdExceeded_shouldLock() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("7.7.7.7");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pipelineReturns(List.of(1L, Boolean.TRUE, 10L, Boolean.TRUE));

        service.recordLoginAttemptAsync("alice", false, "bad password", request);

        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.USER_ACCOUNT_LOCKED), anyString(), any());
    }

    @Test
    void recordLoginAttemptAsync_successful_shouldClearAndAudit() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("5.5.5.5");
        when(request.getHeader("User-Agent")).thenReturn("JUnit-UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:suspicious:alice")).thenReturn(null);
        pipelineReturns(List.of());

        service.recordLoginAttemptAsync("alice", true, null, request);

        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.USER_LOGIN_SUCCESS), anyString(), eq(request));
    }

    @Test
    void recordLoginAttemptAsync_successful_shouldFlagSuspiciousActivity() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("5.5.5.5");
        when(request.getHeader("User-Agent")).thenReturn("NewDevice-UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:suspicious:alice")).thenReturn(Map.of(
                "ipAddress", "1.1.1.1", "userAgent", "OldDevice-UA"));
        pipelineReturns(List.of());

        service.recordLoginAttemptAsync("alice", true, null, request);

        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.SUSPICIOUS_ACTIVITY), anyString(), isNull());
    }

    @Test
    void recordLoginAttemptAsync_nullRequest_shouldComplete() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pipelineReturns(List.of());

        CompletableFuture<Void> result =
                service.recordLoginAttemptAsync("alice", true, null, null);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void recordLoginAttemptAsync_shouldReturnFailedFutureOnError() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9");
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("redis down"));

        CompletableFuture<Void> result =
                service.recordLoginAttemptAsync("alice", false, "bad", request);

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void isAccountLocked_shouldReflectRedisKey() {
        when(redisTemplate.hasKey("auth:account_locked:alice")).thenReturn(true);
        assertTrue(service.isAccountLocked("alice"));

        when(redisTemplate.hasKey("auth:account_locked:bob")).thenReturn(false);
        assertFalse(service.isAccountLocked("bob"));
    }

    @Test
    void getAccountLockInfo_shouldReturnStoredMap() {
        Map<String, Object> info = Map.of("reason", "locked");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:account_locked:alice")).thenReturn(info);
        assertEquals(info, service.getAccountLockInfo("alice"));
    }

    @Test
    void unlockAccount_shouldClearKeysAndAudit() {
        pipelineReturns(List.of());

        service.unlockAccount("alice");

        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.USER_ACCOUNT_UNLOCKED), anyString(), isNull());
    }

    @Test
    void getFailedAttemptCount_shouldReturnStoredNumber() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:failed_attempts:alice")).thenReturn(3);
        assertEquals(3, service.getFailedAttemptCount("alice"));
    }

    @Test
    void getFailedAttemptCount_shouldReturnZeroWhenAbsent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:failed_attempts:ghost")).thenReturn(null);
        assertEquals(0, service.getFailedAttemptCount("ghost"));
    }
    @Test
    void recordLoginAttemptAsync_nullPipelineCount_shouldTreatAsZero() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(request.getHeader("User-Agent")).thenReturn("UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pipelineReturns(Arrays.asList(null, Boolean.TRUE, 1L, Boolean.TRUE));

        CompletableFuture<Void> result =
                service.recordLoginAttemptAsync("alice", false, "bad", request);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void checkSuspiciousActivity_firstLoginEver_shouldNotFlagSuspicious() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        when(request.getHeader("User-Agent")).thenReturn("UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:suspicious:newuser")).thenReturn(null);
        pipelineReturns(List.of());

        service.recordLoginAttemptAsync("newuser", true, null, request);

        verify(auditService, never()).logAuthEvent(
                isNull(), eq("newuser"), eq(AuditLog.AuditAction.SUSPICIOUS_ACTIVITY), anyString(), any());
    }

    @Test
    void recordLoginAttemptAsync_shouldUseXRealIpWhenForwardedForAbsent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("6.6.6.6");
        when(request.getHeader("User-Agent")).thenReturn("UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pipelineReturns(List.of());

        service.recordLoginAttemptAsync("alice", true, null, request);

        verify(auditService).logAuthEvent(isNull(), eq("alice"),
                eq(AuditLog.AuditAction.USER_LOGIN_SUCCESS), anyString(), eq(request));
    }

    @Test
    void recordLoginAttemptAsync_saveDetailsThrows_shouldStillComplete() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        when(request.getHeader("User-Agent")).thenReturn("UA");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doThrow(new RuntimeException("redis write fail"))
                .when(valueOps).set(startsWith("auth:attempt_details:"), any(), any(Duration.class));
        pipelineReturns(List.of());

        CompletableFuture<Void> result =
                service.recordLoginAttemptAsync("alice", true, null, request);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }
}
