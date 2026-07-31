package com.example.ecommerce.service;

import com.example.ecommerce.auth.security.JwtUtils;
import com.example.ecommerce.auth.service.JwtBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtBlacklistServiceTest {

    private static final String TOKEN = "header.payload.signature";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private SetOperations<String, Object> setOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;
    @Mock
    private RedisConnectionFactory connectionFactory;
    @Mock
    private RedisConnection connection;

    private JwtBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new JwtBlacklistService(redisTemplate, jwtUtils);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runCallbacksAgainstConnection() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(inv -> ((RedisCallback) inv.getArgument(0)).doInRedis(connection));
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenAnswer(inv -> {
                    ((RedisCallback) inv.getArgument(0)).doInRedis(connection);
                    return List.of();
                });
    }

    private Instant future() {
        return Instant.now().plusSeconds(3600);
    }

    @Test
    void blacklistToken_shouldStoreKeyWhenTtlPositive() {
        when(jwtUtils.getExpirationDate(TOKEN)).thenReturn(future());
        when(jwtUtils.getUsername(TOKEN)).thenReturn("alice");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(connection.sMembers(any())).thenReturn(Set.of(
                ("someHash:" + future().getEpochSecond()).getBytes(StandardCharsets.UTF_8)));
        runCallbacksAgainstConnection();

        service.blacklistToken(TOKEN);

        verify(valueOps).set(anyString(), eq("blacklisted"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void blacklistToken_shouldSkipWhenTokenAlreadyExpired() {
        when(jwtUtils.getExpirationDate(TOKEN)).thenReturn(Instant.now().minusSeconds(60));

        service.blacklistToken(TOKEN);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blacklistToken_shouldWrapErrorsInIllegalState() {
        assertThrows(IllegalStateException.class, () -> service.blacklistToken(null));
    }

    @Test
    void isTokenBlacklisted_shouldReturnTrueForHashedKey() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        assertTrue(service.isTokenBlacklisted(TOKEN));
    }

    @Test
    void isTokenBlacklisted_shouldReturnTrueForLegacyRawKey() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false, true);
        assertTrue(service.isTokenBlacklisted(TOKEN));
    }

    @Test
    void isTokenBlacklisted_shouldReturnFalseWhenAbsent() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false, false);
        assertFalse(service.isTokenBlacklisted(TOKEN));
    }

    @Test
    void isTokenBlacklisted_shouldFailClosedOnError() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        assertTrue(service.isTokenBlacklisted(TOKEN));
    }

    @Test
    void blacklistUserTokens_shouldBlacklistAllAndClearList() {
        when(connection.sMembers(any())).thenReturn(Set.of(
                ("hashA:" + future().getEpochSecond()).getBytes(StandardCharsets.UTF_8)));
        runCallbacksAgainstConnection();

        service.blacklistUserTokens("alice");

        verify(redisTemplate).delete("jwt:user_tokens:alice");
    }

    @Test
    void blacklistUserTokens_shouldDoNothingWhenNoTokens() {
        when(connection.sMembers(any())).thenReturn(Set.of());
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(inv -> ((RedisCallback<?>) inv.getArgument(0)).doInRedis(connection));

        service.blacklistUserTokens("bob");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void blacklistUserTokens_shouldWrapErrors() {
        when(connection.sMembers(any())).thenReturn(Set.of(
                ("hashA:" + future().getEpochSecond()).getBytes(StandardCharsets.UTF_8)));
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(inv -> ((RedisCallback<?>) inv.getArgument(0)).doInRedis(connection));
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("pipeline failed"));

        assertThrows(IllegalStateException.class, () -> service.blacklistUserTokens("alice"));
    }

    @Test
    void storeTokenMetadata_shouldPersistWhenTtlPositive() {
        when(jwtUtils.getTokenId(TOKEN)).thenReturn("tid-1");
        when(jwtUtils.getExpirationDate(TOKEN)).thenReturn(future());
        runCallbacksAgainstConnection();

        CompletableFuture<Void> result =
                service.storeTokenMetadata(TOKEN, "alice", "1.2.3.4", "JUnit-UA");

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        verify(redisTemplate).executePipelined(any(RedisCallback.class));
    }

    @Test
    void storeTokenMetadata_shouldSkipPipelineWhenExpired() {
        when(jwtUtils.getTokenId(TOKEN)).thenReturn("tid-1");
        when(jwtUtils.getExpirationDate(TOKEN)).thenReturn(Instant.now().minusSeconds(30));

        CompletableFuture<Void> result =
                service.storeTokenMetadata(TOKEN, "alice", "1.2.3.4", "JUnit-UA");

        assertTrue(result.isDone());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void storeTokenMetadata_shouldReturnFailedFutureOnError() {
        when(jwtUtils.getTokenId(TOKEN)).thenThrow(new RuntimeException("bad token"));

        CompletableFuture<Void> result =
                service.storeTokenMetadata(TOKEN, "alice", "1.2.3.4", "JUnit-UA");

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void getTokenMetadata_shouldReturnMappedEntries() {
        when(jwtUtils.getTokenId(TOKEN)).thenReturn("tid-1");
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("jwt:metadata:tid-1")).thenReturn(Map.of("username", "alice"));

        Map<String, Object> metadata = service.getTokenMetadata(TOKEN);

        assertEquals("alice", metadata.get("username"));
    }

    @Test
    void getTokenMetadata_shouldReturnEmptyOnError() {
        when(jwtUtils.getTokenId(TOKEN)).thenThrow(new RuntimeException("boom"));
        assertTrue(service.getTokenMetadata(TOKEN).isEmpty());
    }

    @Test
    void getUserTokens_shouldReturnReferences() {
        when(connection.sMembers(any())).thenReturn(Set.of(
                "refA".getBytes(StandardCharsets.UTF_8),
                "refB".getBytes(StandardCharsets.UTF_8)));
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(inv -> ((RedisCallback<?>) inv.getArgument(0)).doInRedis(connection));

        Set<String> tokens = service.getUserTokens("alice");

        assertEquals(2, tokens.size());
        assertTrue(tokens.contains("refA"));
    }

    @Test
    void getUserTokens_shouldReturnEmptyOnError() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("redis down"));
        assertTrue(service.getUserTokens("alice").isEmpty());
    }

    @Test
    void getUserActiveTokenCount_shouldReturnSize() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size("jwt:user_tokens:alice")).thenReturn(3L);
        assertEquals(3L, service.getUserActiveTokenCount("alice"));
    }

    @Test
    void getUserActiveTokenCount_shouldReturnZeroWhenNull() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size(anyString())).thenReturn(null);
        assertEquals(0L, service.getUserActiveTokenCount("alice"));
    }

    @Test
    void getUserActiveTokenCount_shouldReturnZeroOnError() {
        when(redisTemplate.opsForSet()).thenThrow(new RuntimeException("boom"));
        assertEquals(0L, service.getUserActiveTokenCount("alice"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAllUserTokenStats_shouldReturnPerUserCounts() {
        Cursor<byte[]> cursor = org.mockito.Mockito.mock(Cursor.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        org.mockito.Mockito.doReturn(cursor).when(connection).scan(any(org.springframework.data.redis.core.ScanOptions.class));
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("jwt:user_tokens:alice".getBytes(StandardCharsets.UTF_8));
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size("jwt:user_tokens:alice")).thenReturn(2L);

        Map<String, Long> stats = service.getAllUserTokenStats();

        assertEquals(2L, stats.get("alice"));
    }

    @Test
    void getAllUserTokenStats_shouldReturnEmptyWhenNoConnectionFactory() {
        when(redisTemplate.getConnectionFactory()).thenReturn(null);
        assertTrue(service.getAllUserTokenStats().isEmpty());
    }

    @Test
    void getAllUserTokenStats_shouldReturnEmptyOnError() {
        when(redisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("boom"));
        assertTrue(service.getAllUserTokenStats().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void cleanupExpiredMetadata_shouldDeleteExpiredKeys() {
        Cursor<byte[]> cursor = org.mockito.Mockito.mock(Cursor.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        org.mockito.Mockito.doReturn(cursor).when(connection).scan(any(org.springframework.data.redis.core.ScanOptions.class));
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("jwt:metadata:tid-1".getBytes(StandardCharsets.UTF_8));
        when(redisTemplate.getExpire("jwt:metadata:tid-1", TimeUnit.SECONDS)).thenReturn(0L);

        service.cleanupExpiredMetadata();

        verify(redisTemplate).delete("jwt:metadata:tid-1");
    }

    @Test
    void cleanupExpiredMetadata_shouldReturnWhenNoConnectionFactory() {
        when(redisTemplate.getConnectionFactory()).thenReturn(null);
        service.cleanupExpiredMetadata();
        verify(redisTemplate, never()).delete(anyString());
    }
}
