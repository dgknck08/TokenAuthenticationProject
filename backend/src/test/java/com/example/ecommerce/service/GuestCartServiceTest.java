package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.model.GuestCart;
import com.example.ecommerce.cart.service.GuestCartService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class GuestCartServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService inventoryService;

    private GuestCartService guestCartService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        guestCartService = new GuestCartService(redisTemplate, productRepository, inventoryService);
    }

    @Test
    void getGuestCart_whenNotExists_createsNewCart() {
        when(valueOperations.get("guest_cart:sess-1")).thenReturn(null);

        CartDto dto = guestCartService.getGuestCart("sess-1");

        assertEquals("guest", dto.getCartType());
        assertEquals(0, dto.getTotalItems());
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("guest_cart:sess-1"), org.mockito.ArgumentMatchers.any(GuestCart.class), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void addItemToGuestCart_addsProductAndReturnsTotals() {
        Product product = new Product(5L, "Bag", "desc", new BigDecimal("30.00"), "img", "Cat", 10);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(valueOperations.get("guest_cart:sess-2")).thenReturn(null);

        CartDto dto = guestCartService.addItemToGuestCart("sess-2", 5L, 3);

        assertEquals(3, dto.getTotalItems());
        assertEquals(new BigDecimal("90.00"), dto.getTotalAmount());
        verify(inventoryService).ensureAvailableStock(5L, 3);
    }
}
