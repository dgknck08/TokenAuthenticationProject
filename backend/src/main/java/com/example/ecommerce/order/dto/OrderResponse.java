package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentMethod;
import com.example.ecommerce.order.model.PaymentProvider;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import com.example.ecommerce.order.model.ShippingMethod;
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
    private PaymentProvider paymentProvider;
    private PaymentProviderStatus paymentProviderStatus;
    private String paymentConversationId;
    private String paymentReferenceId;
    private String paymentErrorMessage;
    private BigDecimal totalAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private String couponCode;
    private ShippingMethod shippingMethod;
    private String shippingFullName;
    private String shippingEmail;
    private String shippingPhone;
    private String shippingAddressLine;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    private String trackingNumber;
    private String cancelReason;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant paymentInitializedAt;
    private Instant paymentFailedAt;
    private Instant packedAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant refundedAt;
}
