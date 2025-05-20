package com.example.ecommerce.cart.service;

import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.cart.model.Cart;
import com.example.ecommerce.cart.model.CartItem;
import com.example.ecommerce.cart.repository.CartRepository;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;





import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    @Override
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            var user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    @Override
    public void addItemToCart(Long userId, CartItem item) {
        Cart cart = getCartByUserId(userId);
        cart.addItem(item);
        cartRepository.save(cart);
    }

    @Override
    public void removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
