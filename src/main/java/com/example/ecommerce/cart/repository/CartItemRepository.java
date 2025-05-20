package com.example.ecommerce.cart.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerce.cart.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
}

