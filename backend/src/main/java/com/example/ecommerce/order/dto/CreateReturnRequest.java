package com.example.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReturnRequest {
    @NotBlank(message = "Return reason is required.")
    @Size(max = 500, message = "Return reason is too long.")
    private String reason;
}
