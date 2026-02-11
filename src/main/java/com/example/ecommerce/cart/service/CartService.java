package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.CartDto;

public interface CartService {
    
    // Authenticated user methods
    CartDto getCartByUserId(Long userId);
    CartDto addItemToCart(Long userId, Long productId, int quantity);
    CartDto updateCartItem(Long userId, Long productId, int quantity);
    CartDto removeItemFromCart(Long userId, Long productId);
    void clearCart(Long userId);
    
    // Guest user methods
    CartDto getGuestCart(String sessionId);
    CartDto addItemToGuestCart(String sessionId, Long productId, int quantity);
    CartDto updateGuestCartItem(String sessionId, Long productId, int quantity);
    CartDto removeItemFromGuestCart(String sessionId, Long productId);
    void clearGuestCart(String sessionId);
    
    // Merge functionality
    CartDto mergeGuestCartToUserCart(String sessionId, Long userId);
}