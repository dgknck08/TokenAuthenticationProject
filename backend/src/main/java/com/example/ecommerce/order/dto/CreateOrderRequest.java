package com.example.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {
    @Valid
    @NotEmpty(message = "Order must contain at least one item.")
    private List<OrderItemRequest> items;
}
