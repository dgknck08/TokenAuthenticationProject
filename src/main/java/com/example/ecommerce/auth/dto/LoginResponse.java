package com.example.ecommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record LoginResponse(
    String accessToken,
    
    @JsonIgnore
    String refreshToken,
    
    String username,
    String email
) {}
