package com.example.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipOrderRequest {
    @NotBlank(message = "Tracking number is required.")
    @Size(max = 120, message = "Tracking number is too long.")
    private String trackingNumber;
}
