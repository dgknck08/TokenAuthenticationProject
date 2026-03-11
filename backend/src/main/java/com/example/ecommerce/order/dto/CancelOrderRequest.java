package com.example.ecommerce.order.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {
    @Size(max = 250, message = "Cancel reason is too long.")
    private String reason;
}
