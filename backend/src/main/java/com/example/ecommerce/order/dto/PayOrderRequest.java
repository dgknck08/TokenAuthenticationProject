package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayOrderRequest {
    @NotNull(message = "Payment method is required.")
    private PaymentMethod paymentMethod;
}
