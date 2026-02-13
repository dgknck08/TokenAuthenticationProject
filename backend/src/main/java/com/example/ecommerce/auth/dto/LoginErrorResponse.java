package com.example.ecommerce.auth.dto;

public record LoginErrorResponse(String errorCode, String message) {

	public LoginErrorResponse(String message) {
        this("GENERIC_ERROR", message);
    }
}