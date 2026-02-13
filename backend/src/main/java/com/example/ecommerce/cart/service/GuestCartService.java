package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.model.GuestCart;
import com.example.ecommerce.cart.model.GuestCartItem;
import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.dto.CartItemDto;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCartService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private static final String GUEST_CART_KEY_PREFIX = "guest_cart:";
    private static final Duration CART_EXPIRATION = Duration.ofDays(7); // 7 gün
    
    public CartDto getGuestCart(String sessionId) {
        GuestCart guestCart = getOrCreateGuestCart(sessionId);
        return convertToCartDto(guestCart);
    }
    
    public CartDto addItemToGuestCart(String sessionId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        inventoryService.ensureAvailableStock(productId, quantity);
        
        GuestCart guestCart = getOrCreateGuestCart(sessionId);
        guestCart.addItem(productId, quantity, product.getPrice(), product.getName());
        
        saveGuestCart(sessionId, guestCart);
        log.info("Added {} units of product {} to guest cart {}", quantity, productId, sessionId);
        
        return convertToCartDto(guestCart);
    }
    
    public CartDto updateGuestCartItem(String sessionId, Long productId, int quantity) {
        if (quantity > 0) {
            inventoryService.ensureAvailableStock(productId, quantity);
        }
        GuestCart guestCart = getOrCreateGuestCart(sessionId);
        guestCart.updateItemQuantity(productId, quantity);
        
        saveGuestCart(sessionId, guestCart);
        log.info("Updated product {} quantity to {} in guest cart {}", productId, quantity, sessionId);
        
        return convertToCartDto(guestCart);
    }
    
    public CartDto removeItemFromGuestCart(String sessionId, Long productId) {
        GuestCart guestCart = getOrCreateGuestCart(sessionId);
        guestCart.removeItem(productId);
        
        saveGuestCart(sessionId, guestCart);
        log.info("Removed product {} from guest cart {}", productId, sessionId);
        
        return convertToCartDto(guestCart);
    }
    
    public void clearGuestCart(String sessionId) {
        String key = GUEST_CART_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.info("Cleared guest cart {}", sessionId);
    }
    
    public GuestCart getGuestCartForMerging(String sessionId) {
        return getOrCreateGuestCart(sessionId);
    }
    
    private GuestCart getOrCreateGuestCart(String sessionId) {
        String key = GUEST_CART_KEY_PREFIX + sessionId;
        GuestCart guestCart = (GuestCart) redisTemplate.opsForValue().get(key);
        
        if (guestCart == null) {
            guestCart = new GuestCart();
            guestCart.setSessionId(sessionId);
            guestCart.setCreatedAt(LocalDateTime.now());
            guestCart.setUpdatedAt(LocalDateTime.now());
            saveGuestCart(sessionId, guestCart);
        }
        
        return guestCart;
    }
    
    private void saveGuestCart(String sessionId, GuestCart guestCart) {
        String key = GUEST_CART_KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, guestCart, CART_EXPIRATION);
    }
    
    private CartDto convertToCartDto(GuestCart guestCart) {
        List<CartItemDto> itemDtos = guestCart.getItems().values().stream()
                .map(this::convertToCartItemDto)
                .collect(Collectors.toList());
        
        CartDto cartDto = new CartDto();
        cartDto.setItems(itemDtos);
        cartDto.setTotalItems(guestCart.getTotalItems());
        cartDto.setTotalAmount(guestCart.getTotalAmount());
        cartDto.setCartType("guest");
        
        return cartDto;
    }
    
    private CartItemDto convertToCartItemDto(GuestCartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }
}
