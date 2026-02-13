package com.example.ecommerce.cart.service;

import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.cart.model.Cart;
import com.example.ecommerce.cart.model.CartItem;
import com.example.ecommerce.cart.model.GuestCart;
import com.example.ecommerce.cart.model.GuestCartItem;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.dto.CartItemDto;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartServiceImpl implements CartService {
    
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final GuestCartService guestCartService;

    // Authenticated user methods
    @Override
    public CartDto getCartByUserId(Long userId) {
        Cart cart = getOrCreateUserCart(userId);
        return convertToCartDto(cart);
    }

    @Override
    public CartDto addItemToCart(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateUserCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            // Add new item
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(product.getPrice());
            cart.addItem(newItem);
        }

        cartRepository.save(cart);
        log.info("Added {} units of product {} to user {} cart", quantity, productId, userId);
        
        return convertToCartDto(cart);
    }

    @Override
    public CartDto updateCartItem(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateUserCart(userId);
        
        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        } else {
            // Update quantity
            cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst()
                    .ifPresent(item -> item.setQuantity(quantity));
        }

        cartRepository.save(cart);
        log.info("Updated product {} quantity to {} in user {} cart", productId, quantity, userId);
        
        return convertToCartDto(cart);
    }

    @Override
    public CartDto removeItemFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateUserCart(userId);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
        
        log.info("Removed product {} from user {} cart", productId, userId);
        return convertToCartDto(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateUserCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Cleared cart for user {}", userId);
    }

    // Guest user methods - delegate to GuestCartService
    @Override
    public CartDto getGuestCart(String sessionId) {
        return guestCartService.getGuestCart(sessionId);
    }

    @Override
    public CartDto addItemToGuestCart(String sessionId, Long productId, int quantity) {
        return guestCartService.addItemToGuestCart(sessionId, productId, quantity);
    }

    @Override
    public CartDto updateGuestCartItem(String sessionId, Long productId, int quantity) {
        return guestCartService.updateGuestCartItem(sessionId, productId, quantity);
    }

    @Override
    public CartDto removeItemFromGuestCart(String sessionId, Long productId) {
        return guestCartService.removeItemFromGuestCart(sessionId, productId);
    }

    @Override
    public void clearGuestCart(String sessionId) {
        guestCartService.clearGuestCart(sessionId);
    }

    // Merge functionality
    @Override
    public CartDto mergeGuestCartToUserCart(String sessionId, Long userId) {
        GuestCart guestCart = guestCartService.getGuestCartForMerging(sessionId);
        Cart userCart = getOrCreateUserCart(userId);

        // Merge each item from guest cart to user cart
        for (GuestCartItem guestItem : guestCart.getItems().values()) {
            Product product = productRepository.findById(guestItem.getProductId())
                    .orElse(null);
            
            if (product == null) continue;

            Optional<CartItem> existingItem = userCart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(guestItem.getProductId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Add quantities together
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + guestItem.getQuantity());
            } else {
                // Add new item
                CartItem newItem = new CartItem();
                newItem.setProduct(product);
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setUnitPrice(guestItem.getUnitPrice());
                userCart.addItem(newItem);
            }
        }

        cartRepository.save(userCart);
        
        // Clear guest cart after merging
        guestCartService.clearGuestCart(sessionId);
        
        log.info("Merged guest cart {} to user {} cart", sessionId, userId);
        return convertToCartDto(userCart);
    }

    // Helper methods
    private Cart getOrCreateUserCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    private CartDto convertToCartDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::convertToCartItemDto)
                .collect(Collectors.toList());

        CartDto cartDto = new CartDto();
        cartDto.setItems(itemDtos);
        cartDto.setTotalItems(cart.getItems().stream().mapToInt(CartItem::getQuantity).sum());
        cartDto.setTotalAmount(cart.getTotalAmount());
        cartDto.setCartType("authenticated");

        return cartDto;
    }

    private CartItemDto convertToCartItemDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        // dto.setProductImage(item.getProduct().getImage()); // Product model'inde image field'i varsa
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }
}