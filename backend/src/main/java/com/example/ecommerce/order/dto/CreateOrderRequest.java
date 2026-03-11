package com.example.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import com.example.ecommerce.order.model.ShippingMethod;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {
    @Valid
    @NotEmpty(message = "Order must contain at least one item.")
    private List<OrderItemRequest> items;

    private String couponCode;

    private ShippingMethod shippingMethod;

    @Valid
    private ShippingAddressRequest shippingAddress;
}
