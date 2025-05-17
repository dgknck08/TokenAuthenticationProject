package com.example.ecommerce.auth.exception;


public class TokenRefreshException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String token;

    public TokenRefreshException(String token, String message) {
        super(message);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}

