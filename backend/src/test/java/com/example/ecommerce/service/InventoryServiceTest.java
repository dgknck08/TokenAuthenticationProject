package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ecommerce.cart.exception.InsufficientStockException;
import com.example.ecommerce.inventory.model.InventoryItem;
import com.example.ecommerce.inventory.repository.InventoryRepository;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, productRepository);
    }

    @Test
    void initializeStock_createsInventoryRecord() {
        Product product = new Product(1L, "Phone", "Desc", new BigDecimal("100.00"), "img", "Cat", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        inventoryService.initializeStock(1L, 15);

        verify(inventoryRepository).save(org.mockito.ArgumentMatchers.any(InventoryItem.class));
    }

    @Test
    void getAvailableStock_readsFromInventoryItem_whenExists() {
        InventoryItem item = new InventoryItem();
        item.setAvailableStock(9);
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(item));

        int available = inventoryService.getAvailableStock(2L);
        assertEquals(9, available);
    }

    @Test
    void ensureAvailableStock_throwsWhenRequestedExceedsAvailable() {
        InventoryItem item = new InventoryItem();
        item.setAvailableStock(2);
        when(inventoryRepository.findByProductId(3L)).thenReturn(Optional.of(item));

        assertThrows(InsufficientStockException.class, () -> inventoryService.ensureAvailableStock(3L, 5));
    }
}
