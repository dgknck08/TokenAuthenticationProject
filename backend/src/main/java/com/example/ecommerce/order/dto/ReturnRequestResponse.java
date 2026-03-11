package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.model.ReturnRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ReturnRequestResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private String username;
    private ReturnRequestStatus status;
    private String reason;
    private String adminNote;
    private String reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
