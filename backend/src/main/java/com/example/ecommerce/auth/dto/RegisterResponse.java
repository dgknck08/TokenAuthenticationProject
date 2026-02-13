package com.example.ecommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RegisterResponse(
    String accessToken,
    
    @JsonIgnore
    String refreshToken,
    
    String username,
    String email
) {}