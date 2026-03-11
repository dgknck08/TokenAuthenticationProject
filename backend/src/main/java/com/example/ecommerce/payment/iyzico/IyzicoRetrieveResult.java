package com.example.ecommerce.payment.iyzico;

public record IyzicoRetrieveResult(
        boolean success,
        String status,
        String paymentStatus,
        String conversationId,
        String paymentId,
        String errorCode,
        String errorMessage
) {
}
