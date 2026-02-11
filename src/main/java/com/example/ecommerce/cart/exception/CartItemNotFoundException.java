package com.example.ecommerce.cart.exception;

public class CartItemNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CartItemNotFoundException(String message) {
        super(message);
    }

    public CartItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}