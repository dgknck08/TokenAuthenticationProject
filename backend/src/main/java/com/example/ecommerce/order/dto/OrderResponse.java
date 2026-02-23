package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String username;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant cancelledAt;
    private Instant refundedAt;
}
