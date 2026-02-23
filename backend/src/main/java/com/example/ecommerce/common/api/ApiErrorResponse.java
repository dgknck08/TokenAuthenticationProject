package com.example.ecommerce.common.api;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record ApiErrorResponse(
        @Schema(example = "VALIDATION_ERROR") 
        String code,
        @Schema(example = "Invalid payload")
        String message,
        @Schema(example = "2026-02-23T19:41:03Z")
        String timestamp,
        @Schema(example = "/api/orders/1/pay")
        String path
) {
    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, Instant.now().toString(), path);
    }
}
