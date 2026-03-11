package com.example.ecommerce.payment.iyzico.dto;

import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentMethod;
import com.example.ecommerce.order.model.PaymentProvider;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class IyzicoPaymentStatusResponse {
    private Long orderId;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentProvider provider;
    private PaymentProviderStatus paymentStatus;
    private String conversationId;
    private String paymentReferenceId;
    private String paymentErrorMessage;
    private Instant paymentInitializedAt;
    private Instant paidAt;
    private Instant paymentFailedAt;
}
