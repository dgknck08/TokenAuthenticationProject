package com.example.ecommerce.order.exception;

public class ReturnRequestNotFoundException extends RuntimeException {
    public ReturnRequestNotFoundException(String message) {
        super(message);
    }
}
