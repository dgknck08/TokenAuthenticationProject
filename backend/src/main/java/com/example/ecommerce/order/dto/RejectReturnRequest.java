package com.example.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectReturnRequest {
    @NotBlank(message = "Rejection note is required.")
    @Size(max = 500, message = "Rejection note is too long.")
    private String note;
}
