package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.model.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CartItemService {
    
    CartItem getCartItemById(Long id);
    
    CartItem saveCartItem(CartItem item);
    
    void deleteCartItem(Long id);
    
    List<CartItem> getCartItemsByCartId(Long cartId);
    
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    
    void deleteByCartIdAndProductId(Long cartId, Long productId);
    
    void deleteByCartId(Long cartId);
    
    long countByCartId(Long cartId);

	Page<CartItem> getCartItemsByCartId(Long cartId, Pageable pageable);

	List<CartItem> saveAllCartItems(List<CartItem> items);

	CartItem updateUnitPrice(Long cartItemId, BigDecimal newPrice);

	CartItem updateQuantity(Long cartItemId, int newQuantity);

	int calculateCartItemCount(Long cartId);

	BigDecimal calculateCartTotal(Long cartId);

	List<CartItem> findExpiredItems(Long cartId, int maxAgeInDays);

	List<CartItem> refreshItemPrices(Long cartId);
}