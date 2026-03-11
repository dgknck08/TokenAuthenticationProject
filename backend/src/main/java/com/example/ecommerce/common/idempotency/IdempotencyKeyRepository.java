package com.example.ecommerce.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, Long> {
    Optional<IdempotencyKeyRecord> findTopByScopeAndOperationAndIdempotencyKeyOrderByIdDesc(
            String scope,
            String operation,
            String idempotencyKey
    );

    long deleteByExpiresAtBefore(Instant now);
}
