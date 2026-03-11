package com.example.ecommerce.payment.iyzico.dto;

import com.example.ecommerce.order.model.PaymentProviderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IyzicoInitPaymentResponse {
    private PaymentProviderStatus paymentStatus;
    private String paymentPageUrl;
}
