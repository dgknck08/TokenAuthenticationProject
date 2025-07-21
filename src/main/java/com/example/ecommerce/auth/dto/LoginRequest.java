package com.example.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Username cannot be empty")
    String username,
    
    @NotBlank(message = "Password cannot be empty")
    String password
) {}
