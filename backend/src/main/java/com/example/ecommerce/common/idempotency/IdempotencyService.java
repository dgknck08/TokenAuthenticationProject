package com.example.ecommerce.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class IdempotencyService {
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final long ttlHours;

    public IdempotencyService(IdempotencyKeyRepository repository,
                              ObjectMapper objectMapper,
                              @Value("${app.idempotency.ttl-hours:24}") long ttlHours) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.ttlHours = ttlHours;
    }

    @Transactional(readOnly = true)
    public <T> Optional<ResponseEntity<T>> findReplayResponse(String scope,
                                                              String operation,
                                                              String idempotencyKey,
                                                              Object requestPayload,
                                                              Class<T> responseType) {
        String normalizedKey = normalizeKey(idempotencyKey);
        if (normalizedKey == null) {
            return Optional.empty();
        }

        String requestHash = hashPayload(requestPayload);
        Optional<IdempotencyKeyRecord> existingOptional = repository
                .findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc(scope, operation, normalizedKey);
        if (existingOptional.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyKeyRecord existing = existingOptional.get();
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IllegalArgumentException("Idempotency key is already used with a different payload.");
        }
        if (existing.getResponseStatus() == null || existing.getResponseBody() == null) {
            return Optional.empty();
        }

        try {
            T responseBody = objectMapper.readValue(existing.getResponseBody(), responseType);
            return Optional.of(ResponseEntity.status(existing.getResponseStatus()).body(responseBody));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize idempotent response payload", ex);
        }
    }

    public void saveResponse(String scope,
                             String operation,
                             String idempotencyKey,
                             Object requestPayload,
                             int statusCode,
                             Object responseBody) {
        String normalizedKey = normalizeKey(idempotencyKey);
        if (normalizedKey == null) {
            return;
        }

        String requestHash = hashPayload(requestPayload);
        IdempotencyKeyRecord record = repository
                .findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc(scope, operation, normalizedKey)
                .orElseGet(IdempotencyKeyRecord::new);

        if (record.getId() != null && !requestHash.equals(record.getRequestHash())) {
            throw new IllegalArgumentException("Idempotency key is already used with a different payload.");
        }

        try {
            record.setScope(scope);
            record.setOperation(operation);
            record.setIdempotencyKey(normalizedKey);
            record.setRequestHash(requestHash);
            record.setResponseStatus(statusCode);
            record.setResponseBody(objectMapper.writeValueAsString(responseBody));
            record.setExpiresAt(Instant.now().plusSeconds(Math.max(ttlHours, 1) * 3600));
            repository.save(record);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist idempotent response", ex);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredKeys() {
        try {
            long deleted = repository.deleteByExpiresAtBefore(Instant.now());
            if (deleted > 0) {
                logger.info("Deleted {} expired idempotency key(s)", deleted);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup expired idempotency keys", e);
        }
    }

    private String normalizeKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }
        String normalized = rawKey.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Idempotency key must be at most 120 characters.");
        }
        return normalized;
    }

    private String hashPayload(Object payload) {
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(serialized);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash request payload for idempotency", e);
        }
    }
}
