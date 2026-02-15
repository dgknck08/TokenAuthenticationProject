package com.example.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
        regexp = "^(?=.{3,30}$)[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?$",
        message = "Username format is invalid"
    )
    String username,
    
    @NotBlank(message = "Password cannot be empty")
    String password
) {}
