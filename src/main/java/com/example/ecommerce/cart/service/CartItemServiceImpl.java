package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.model.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;






import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;

    @Override
    public CartItem getCartItemById(Long id) {
        return cartItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("CartItem not found"));
    }

    @Override
    public CartItem saveCartItem(CartItem item) {
        return cartItemRepository.save(item);
    }

    @Override
    public void deleteCartItem(Long id) {
        cartItemRepository.deleteById(id);
    }
}
