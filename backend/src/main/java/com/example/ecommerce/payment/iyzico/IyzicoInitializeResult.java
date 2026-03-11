package com.example.ecommerce.payment.iyzico;

public record IyzicoInitializeResult(
        boolean success,
        String status,
        String conversationId,
        String token,
        String paymentPageUrl,
        String checkoutFormContent,
        Long tokenExpireTime,
        String errorCode,
        String errorMessage
) {
}
