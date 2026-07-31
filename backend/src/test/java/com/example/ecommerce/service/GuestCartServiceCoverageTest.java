package com.example.ecommerce.service;

import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.model.GuestCart;
import com.example.ecommerce.cart.service.GuestCartService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuestCartServiceCoverageTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private ValueOperations<String, Object> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GuestCartService service;

    @BeforeEach
    void setUp() {
        service = new GuestCartService(redisTemplate, productRepository, inventoryService, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("Guitar");
        p.setPrice(new BigDecimal("100.00"));
        p.setStock(10);
        return p;
    }

    private GuestCart existingCart(String sessionId) {
        GuestCart cart = new GuestCart();
        cart.setSessionId(sessionId);
        cart.addItem(1L, 1, new BigDecimal("100.00"), "Guitar");
        return cart;
    }

    @Test
    void getGuestCart_shouldCreateNewCartWhenAbsent() {
        when(valueOps.get("guest_cart:s1")).thenReturn(null);

        CartDto dto = service.getGuestCart("s1");

        assertEquals("guest", dto.getCartType());
        assertEquals(0, dto.getTotalItems());
        verify(valueOps).set(eq("guest_cart:s1"), any(GuestCart.class), any());
    }

    @Test
    void addItemToGuestCart_shouldAddWhenStockAvailable() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L)));
        when(valueOps.get("guest_cart:s1")).thenReturn(existingCart("s1"));

        CartDto dto = service.addItemToGuestCart("s1", 1L, 2);

        assertEquals("guest", dto.getCartType());
        verify(inventoryService).ensureAvailableStock(1L, 2);
    }

    @Test
    void addItemToGuestCart_shouldThrowWhenProductMissing() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
                () -> service.addItemToGuestCart("s1", 9L, 1));
    }

    @Test
    void updateGuestCartItem_shouldCheckStockForPositiveQuantity() {
        when(valueOps.get("guest_cart:s1")).thenReturn(existingCart("s1"));

        service.updateGuestCartItem("s1", 1L, 3);

        verify(inventoryService).ensureAvailableStock(1L, 3);
    }

    @Test
    void updateGuestCartItem_shouldRemoveWhenQuantityZero() {
        when(valueOps.get("guest_cart:s1")).thenReturn(existingCart("s1"));

        CartDto dto = service.updateGuestCartItem("s1", 1L, 0);

        assertEquals(0, dto.getTotalItems());
    }

    @Test
    void removeItemFromGuestCart_shouldRemoveItem() {
        when(valueOps.get("guest_cart:s1")).thenReturn(existingCart("s1"));

        CartDto dto = service.removeItemFromGuestCart("s1", 1L);

        assertEquals(0, dto.getTotalItems());
    }

    @Test
    void clearGuestCart_shouldDeleteKey() {
        service.clearGuestCart("s1");
        verify(redisTemplate).delete("guest_cart:s1");
    }

    @Test
    void getGuestCartForMerging_shouldReturnCart() {
        when(valueOps.get("guest_cart:s1")).thenReturn(existingCart("s1"));
        GuestCart cart = service.getGuestCartForMerging("s1");
        assertEquals(1, cart.getTotalItems());
    }

    @Test
    void getOrCreate_shouldRecoverLegacyPayload() {
        // A plain map (legacy payload without type metadata) triggers convertValue recovery.
        java.util.Map<String, Object> legacy = new java.util.HashMap<>();
        legacy.put("sessionId", "s1");
        when(valueOps.get("guest_cart:s1")).thenReturn(legacy);

        CartDto dto = service.getGuestCart("s1");

        assertEquals("guest", dto.getCartType());
    }
}
