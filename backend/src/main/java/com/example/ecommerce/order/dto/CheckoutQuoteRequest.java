package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.model.ShippingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutQuoteRequest {
    @Valid
    @NotEmpty(message = "Quote must contain at least one item.")
    private List<OrderItemRequest> items;

    private String couponCode;

    private ShippingMethod shippingMethod;
}
