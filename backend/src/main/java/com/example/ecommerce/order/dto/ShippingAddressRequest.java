package com.example.ecommerce.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressRequest {
    @Size(max = 120, message = "Shipping full name is too long.")
    private String fullName;

    @Email(message = "Shipping email format is invalid.")
    @Size(max = 120, message = "Shipping email is too long.")
    private String email;

    @Size(max = 40, message = "Shipping phone is too long.")
    private String phone;

    @Size(max = 255, message = "Shipping address is too long.")
    private String addressLine;

    @Size(max = 120, message = "Shipping city is too long.")
    private String city;

    @Size(max = 40, message = "Shipping postal code is too long.")
    private String postalCode;

    @Size(max = 120, message = "Shipping country is too long.")
    private String country;
}
