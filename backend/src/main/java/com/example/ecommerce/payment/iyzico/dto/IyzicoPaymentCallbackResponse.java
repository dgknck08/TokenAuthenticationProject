package com.example.ecommerce.payment.iyzico.dto;

import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IyzicoPaymentCallbackResponse {
    private Long orderId;
    private String conversationId;
    private String paymentReferenceId;
    private PaymentProviderStatus paymentStatus;
    private OrderStatus orderStatus;
    private boolean success;
    private String message;
}
