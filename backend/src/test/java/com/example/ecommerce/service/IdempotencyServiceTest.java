package com.example.ecommerce.service;

import com.example.ecommerce.common.idempotency.IdempotencyKeyRecord;
import com.example.ecommerce.common.idempotency.IdempotencyKeyRepository;
import com.example.ecommerce.common.idempotency.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, objectMapper, 24);
    }

    private String hash(Object payload) throws Exception {
        byte[] serialized = objectMapper.writeValueAsBytes(payload);
        byte[] hashed = MessageDigest.getInstance("SHA-256").digest(serialized);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    }

    private IdempotencyKeyRecord record(String requestHash, Integer status, String body, Instant expiresAt) {
        IdempotencyKeyRecord r = new IdempotencyKeyRecord();
        r.setId(1L);
        r.setRequestHash(requestHash);
        r.setResponseStatus(status);
        r.setResponseBody(body);
        r.setExpiresAt(expiresAt);
        return r;
    }

    // ── findReplayResponse ────────────────────────────────────
    @Test
    void findReplayResponse_shouldReturnEmptyWhenKeyNull() {
        assertTrue(service.findReplayResponse("s", "op", null, "payload", String.class).isEmpty());
    }

    @Test
    void findReplayResponse_shouldReturnEmptyWhenNoRecord() {
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.empty());
        assertTrue(service.findReplayResponse("s", "op", "k", "payload", String.class).isEmpty());
    }

    @Test
    void findReplayResponse_shouldReturnEmptyWhenExpired() throws Exception {
        IdempotencyKeyRecord r = record(hash("payload"), 200, "\"ok\"", Instant.now().minusSeconds(60));
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.of(r));
        assertTrue(service.findReplayResponse("s", "op", "k", "payload", String.class).isEmpty());
    }

    @Test
    void findReplayResponse_shouldThrowOnPayloadMismatch() {
        IdempotencyKeyRecord r = record("differentHash", 200, "\"ok\"", Instant.now().plusSeconds(60));
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.of(r));
        assertThrows(IllegalArgumentException.class,
                () -> service.findReplayResponse("s", "op", "k", "payload", String.class));
    }

    @Test
    void findReplayResponse_shouldReturnEmptyWhenResponseIncomplete() throws Exception {
        IdempotencyKeyRecord r = record(hash("payload"), null, null, Instant.now().plusSeconds(60));
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.of(r));
        assertTrue(service.findReplayResponse("s", "op", "k", "payload", String.class).isEmpty());
    }

    @Test
    void findReplayResponse_shouldReplayStoredResponse() throws Exception {
        IdempotencyKeyRecord r = record(hash("payload"), 201, "\"created\"", Instant.now().plusSeconds(60));
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.of(r));

        Optional<ResponseEntity<String>> replay =
                service.findReplayResponse("s", "op", "k", "payload", String.class);

        assertTrue(replay.isPresent());
        assertEquals(201, replay.get().getStatusCode().value());
        assertEquals("created", replay.get().getBody());
    }

    @Test
    void findReplayResponse_shouldThrowWhenBodyCannotDeserialize() throws Exception {
        IdempotencyKeyRecord r = record(hash("payload"), 200, "not-json", Instant.now().plusSeconds(60));
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.of(r));
        assertThrows(IllegalStateException.class,
                () -> service.findReplayResponse("s", "op", "k", "payload", Integer.class));
    }

    @Test
    void findReplayResponse_shouldRejectTooLongKey() {
        String longKey = "x".repeat(121);
        assertThrows(IllegalArgumentException.class,
                () -> service.findReplayResponse("s", "op", longKey, "payload", String.class));
    }

    // ── saveResponse ──────────────────────────────────────────
    @Test
    void saveResponse_shouldSkipWhenKeyNull() {
        service.saveResponse("s", "op", null, "payload", 200, "body");
        verify(repository, never()).save(any());
    }

    @Test
    void saveResponse_shouldPersistNewRecord() {
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.empty());

        service.saveResponse("s", "op", "k", "payload", 201, "created");

        verify(repository).save(any(IdempotencyKeyRecord.class));
    }

    @Test
    void saveResponse_shouldRejectDifferentPayloadForExistingKey() {
        IdempotencyKeyRecord existing = record("differentHash", 200, "\"ok\"", Instant.now().plusSeconds(60));
        when(repository.findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc("s", "op", "k"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.saveResponse("s", "op", "k", "payload", 200, "body"));
        verify(repository, never()).save(any());
    }

    // ── cleanupExpiredKeys ────────────────────────────────────
    @Test
    void cleanupExpiredKeys_shouldDeleteExpired() {
        when(repository.deleteByExpiresAtBefore(any())).thenReturn(3L);
        service.cleanupExpiredKeys();
        verify(repository).deleteByExpiresAtBefore(any());
    }

    @Test
    void cleanupExpiredKeys_shouldSwallowErrors() {
        when(repository.deleteByExpiresAtBefore(any())).thenThrow(new RuntimeException("db down"));
        service.cleanupExpiredKeys();
        verify(repository).deleteByExpiresAtBefore(any());
    }
}
