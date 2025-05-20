package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.model.CartItem;

public interface CartItemService {
    CartItem getCartItemById(Long id);
    CartItem saveCartItem(CartItem item);
    void deleteCartItem(Long id);
}