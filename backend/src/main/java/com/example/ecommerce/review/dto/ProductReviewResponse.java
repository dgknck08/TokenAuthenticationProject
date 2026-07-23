package com.example.ecommerce.review.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ProductReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String username,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
