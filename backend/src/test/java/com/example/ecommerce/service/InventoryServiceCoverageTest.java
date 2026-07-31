package com.example.ecommerce.service;

import com.example.ecommerce.cart.exception.InsufficientStockException;
import com.example.ecommerce.inventory.model.InventoryItem;
import com.example.ecommerce.inventory.repository.InventoryRepository;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceCoverageTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService selfProxy;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(inventoryRepository, productRepository, selfProxy);
    }

    private Product product(Long id, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(stock);
        return p;
    }

    private InventoryItem inventory(Product p, int available) {
        InventoryItem item = new InventoryItem();
        item.setProduct(p);
        item.setAvailableStock(available);
        item.setReorderLevel(5);
        return item;
    }

    @Test
    void initializeStock_shouldCreateItemWhenNoneExists() {
        Product p = product(1L, 0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        service.initializeStock(1L, 25);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        assertEquals(25, captor.getValue().getAvailableStock());
    }

    @Test
    void initializeStock_shouldClampNegativeToZero() {
        Product p = product(1L, 0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(p, 3)));

        service.initializeStock(1L, -10);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getAvailableStock());
    }

    @Test
    void initializeStock_shouldThrowWhenProductMissing() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.initializeStock(9L, 5));
    }

    @Test
    void getAvailableStock_shouldReturnInventoryStock() {
        Product p = product(1L, 100);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(p, 42)));
        assertEquals(42, service.getAvailableStock(1L));
    }

    @Test
    void getAvailableStock_shouldFallBackToProductStock() {
        Product p = product(1L, 7);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        assertEquals(7, service.getAvailableStock(1L));
    }

    @Test
    void getAvailableStock_shouldReturnZeroWhenNothingKnown() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(0, service.getAvailableStock(1L));
    }

    @Test
    void setStock_shouldUpdateProductAndInventory() {
        Product p = product(1L, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        InventoryItem item = inventory(p, 5);
        item.setReorderLevel(0);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        service.setStock(1L, 30);

        assertEquals(30, p.getStock());
        assertEquals(5, item.getReorderLevel());
        verify(inventoryRepository).save(item);
    }

    @Test
    void setStock_shouldThrowWhenProductMissing() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.setStock(9L, 5));
    }

    @Test
    void decreaseStock_shouldReduceAvailable() {
        Product p = product(1L, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        InventoryItem item = inventory(p, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        service.decreaseStockWithOptimisticLock(1L, 4);

        assertEquals(6, item.getAvailableStock());
        assertEquals(6, p.getStock());
        verify(inventoryRepository).saveAndFlush(item);
    }

    @Test
    void decreaseStock_shouldThrowWhenInsufficient() {
        Product p = product(1L, 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(p, 2)));

        assertThrows(InsufficientStockException.class,
                () -> service.decreaseStockWithOptimisticLock(1L, 5));
        verify(inventoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void decreaseStock_shouldCreateInventoryFromProductWhenMissing() {
        Product p = product(1L, 8);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        service.decreaseStockWithOptimisticLock(1L, 3);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).saveAndFlush(captor.capture());
        assertEquals(5, captor.getValue().getAvailableStock());
    }

    @Test
    void increaseStock_shouldAddAvailable() {
        Product p = product(1L, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        InventoryItem item = inventory(p, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        service.increaseStockWithOptimisticLock(1L, 5);

        assertEquals(15, item.getAvailableStock());
        verify(inventoryRepository).saveAndFlush(item);
    }

    @Test
    void ensureAvailableStock_shouldPassWhenEnough() {
        when(selfProxy.getAvailableStock(1L)).thenReturn(10);
        service.ensureAvailableStock(1L, 10);
    }

    @Test
    void ensureAvailableStock_shouldThrowWhenNotEnough() {
        when(selfProxy.getAvailableStock(1L)).thenReturn(2);
        assertThrows(InsufficientStockException.class,
                () -> service.ensureAvailableStock(1L, 5));
    }
}
