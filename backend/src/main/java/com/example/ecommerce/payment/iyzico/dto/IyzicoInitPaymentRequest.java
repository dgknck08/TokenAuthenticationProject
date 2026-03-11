package com.example.ecommerce.payment.iyzico.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IyzicoInitPaymentRequest {
    @Pattern(regexp = "tr|en", message = "locale must be 'tr' or 'en'.")
    private String locale = "tr";
}
