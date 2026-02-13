package com.example.ecommerce.common.api;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        String timestamp,
        String path
) {
    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, Instant.now().toString(), path);
    }
}
