package com.example.ecommerce.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.ecommerce.auth.dto.LoginErrorResponse;

@RestControllerAdvice
public class CartExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<LoginErrorResponse> handleCartNotFound(CartNotFoundException ex) {
        LoginErrorResponse errorResponse = new LoginErrorResponse(
            "CART_NOT_FOUND", 
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<LoginErrorResponse> handleCartItemNotFound(CartItemNotFoundException ex) {
        LoginErrorResponse errorResponse = new LoginErrorResponse(
            "CART_ITEM_NOT_FOUND", 
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<LoginErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        LoginErrorResponse errorResponse = new LoginErrorResponse(
            "INSUFFICIENT_STOCK", 
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}