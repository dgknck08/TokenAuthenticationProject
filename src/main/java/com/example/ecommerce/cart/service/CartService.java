package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.model.Cart;

import com.example.ecommerce.cart.model.CartItem;

public interface CartService {
    Cart getCartByUserId(Long userId);
    void addItemToCart(Long userId, CartItem item);
    void removeItemFromCart(Long userId, Long cartItemId);
    void clearCart(Long userId);
}

