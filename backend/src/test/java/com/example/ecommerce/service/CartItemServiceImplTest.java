package com.example.ecommerce.service;

import com.example.ecommerce.cart.exception.CartItemNotFoundException;
import com.example.ecommerce.cart.exception.CartOperationException;
import com.example.ecommerce.cart.model.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.service.CartItemService;
import com.example.ecommerce.cart.service.CartItemServiceImpl;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartItemServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private CartItemService selfProxy;

    private CartItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CartItemServiceImpl(cartItemRepository, productRepository, inventoryService, selfProxy);
    }

    private Product product(Long id, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    private CartItem item(Long id, int qty, String unitPrice, Product product) {
        CartItem c = new CartItem();
        c.setId(id);
        c.setQuantity(qty);
        c.setUnitPrice(new BigDecimal(unitPrice));
        c.setProduct(product);
        return c;
    }

    @Test
    void getCartItemById_shouldReturnWhenFound() {
        CartItem item = item(1L, 2, "10.00", product(5L, "10.00", 3));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        assertSame(item, service.getCartItemById(1L));
    }

    @Test
    void getCartItemById_shouldThrowWhenNotFound() {
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CartItemNotFoundException.class, () -> service.getCartItemById(99L));
    }

    @Test
    void saveCartItem_shouldPersistValidItem() {
        CartItem item = item(null, 2, "10.00", product(5L, "10.00", 3));
        when(cartItemRepository.save(item)).thenReturn(item);
        assertSame(item, service.saveCartItem(item));
        verify(cartItemRepository).save(item);
    }

    @Test
    void saveCartItem_shouldWrapValidationFailure() {
        CartItem invalid = item(null, 0, "10.00", product(5L, "10.00", 3));
        CartOperationException ex = assertThrows(CartOperationException.class,
                () -> service.saveCartItem(invalid));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void saveCartItem_shouldRejectNullUnitPrice() {
        CartItem invalid = item(null, 1, "0", product(5L, "10.00", 3));
        invalid.setUnitPrice(null);
        assertThrows(CartOperationException.class, () -> service.saveCartItem(invalid));
    }

    @Test
    void saveCartItem_shouldRejectNullProduct() {
        CartItem invalid = item(null, 1, "10.00", null);
        assertThrows(CartOperationException.class, () -> service.saveCartItem(invalid));
    }

    @Test
    void saveCartItem_existingItemTriggersPriceCheck() {
        Product p = product(5L, "12.00", 3);
        CartItem item = item(7L, 1, "10.00", p);
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(cartItemRepository.save(item)).thenReturn(item);
        assertSame(item, service.saveCartItem(item));
    }

    @Test
    void deleteCartItem_shouldDeleteWhenExists() {
        when(cartItemRepository.existsById(1L)).thenReturn(true);
        service.deleteCartItem(1L);
        verify(cartItemRepository).deleteById(1L);
    }

    @Test
    void deleteCartItem_shouldThrowWhenNotExists() {
        when(cartItemRepository.existsById(1L)).thenReturn(false);
        assertThrows(CartItemNotFoundException.class, () -> service.deleteCartItem(1L));
    }

    @Test
    void deleteCartItem_shouldWrapRepositoryError() {
        when(cartItemRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("db")).when(cartItemRepository).deleteById(1L);
        assertThrows(CartOperationException.class, () -> service.deleteCartItem(1L));
    }

    @Test
    void getCartItemsByCartId_shouldReturnItems() {
        List<CartItem> items = List.of(item(1L, 1, "10.00", product(5L, "10.00", 3)));
        when(cartItemRepository.findByCartId(2L)).thenReturn(items);
        assertEquals(1, service.getCartItemsByCartId(2L).size());
    }

    @Test
    void getCartItemsByCartId_shouldWrapError() {
        when(cartItemRepository.findByCartId(2L)).thenThrow(new RuntimeException("db"));
        assertThrows(CartOperationException.class, () -> service.getCartItemsByCartId(2L));
    }

    @Test
    void findByCartIdAndProductId_shouldReturnItem() {
        CartItem item = item(1L, 1, "10.00", product(5L, "10.00", 3));
        when(cartItemRepository.findByCartIdAndProductId(2L, 5L)).thenReturn(Optional.of(item));
        assertTrue(service.findByCartIdAndProductId(2L, 5L).isPresent());
    }

    @Test
    void findByCartIdAndProductId_shouldWrapError() {
        when(cartItemRepository.findByCartIdAndProductId(2L, 5L)).thenThrow(new RuntimeException("db"));
        assertThrows(CartOperationException.class, () -> service.findByCartIdAndProductId(2L, 5L));
    }

    @Test
    void deleteByCartIdAndProductId_shouldDeleteWhenPresent() {
        CartItem item = item(1L, 1, "10.00", product(5L, "10.00", 3));
        when(selfProxy.findByCartIdAndProductId(2L, 5L)).thenReturn(Optional.of(item));
        service.deleteByCartIdAndProductId(2L, 5L);
        verify(cartItemRepository).deleteByCartIdAndProductId(2L, 5L);
    }

    @Test
    void deleteByCartIdAndProductId_shouldThrowWhenAbsent() {
        when(selfProxy.findByCartIdAndProductId(2L, 5L)).thenReturn(Optional.empty());
        assertThrows(CartItemNotFoundException.class,
                () -> service.deleteByCartIdAndProductId(2L, 5L));
    }

    @Test
    void deleteByCartIdAndProductId_shouldWrapRepositoryError() {
        CartItem item = item(1L, 1, "10.00", product(5L, "10.00", 3));
        when(selfProxy.findByCartIdAndProductId(2L, 5L)).thenReturn(Optional.of(item));
        doThrow(new RuntimeException("db")).when(cartItemRepository).deleteByCartIdAndProductId(2L, 5L);
        assertThrows(CartOperationException.class,
                () -> service.deleteByCartIdAndProductId(2L, 5L));
    }

    @Test
    void deleteByCartId_shouldDeleteAll() {
        when(cartItemRepository.countByCartId(2L)).thenReturn(3L);
        service.deleteByCartId(2L);
        verify(cartItemRepository).deleteByCartId(2L);
    }

    @Test
    void deleteByCartId_shouldWrapError() {
        when(cartItemRepository.countByCartId(2L)).thenThrow(new RuntimeException("db"));
        assertThrows(CartOperationException.class, () -> service.deleteByCartId(2L));
    }

    @Test
    void countByCartId_shouldReturnCount() {
        when(cartItemRepository.countByCartId(2L)).thenReturn(4L);
        assertEquals(4L, service.countByCartId(2L));
    }

    @Test
    void countByCartId_shouldWrapError() {
        when(cartItemRepository.countByCartId(2L)).thenThrow(new RuntimeException("db"));
        assertThrows(CartOperationException.class, () -> service.countByCartId(2L));
    }

    @Test
    void calculateCartTotal_shouldSumLineTotals() {
        List<CartItem> items = List.of(
                item(1L, 2, "10.00", product(5L, "10.00", 3)),
                item(2L, 1, "5.50", product(6L, "5.50", 3)));
        when(selfProxy.getCartItemsByCartId(2L)).thenReturn(items);
        assertEquals(new BigDecimal("25.50"), service.calculateCartTotal(2L));
    }

    @Test
    void calculateCartTotal_shouldWrapError() {
        when(selfProxy.getCartItemsByCartId(2L)).thenThrow(new RuntimeException("boom"));
        assertThrows(CartOperationException.class, () -> service.calculateCartTotal(2L));
    }

    @Test
    void calculateCartItemCount_shouldSumQuantities() {
        List<CartItem> items = List.of(
                item(1L, 2, "10.00", product(5L, "10.00", 3)),
                item(2L, 3, "5.00", product(6L, "5.00", 3)));
        when(selfProxy.getCartItemsByCartId(2L)).thenReturn(items);
        assertEquals(5, service.calculateCartItemCount(2L));
    }

    @Test
    void calculateCartItemCount_shouldWrapError() {
        when(selfProxy.getCartItemsByCartId(2L)).thenThrow(new RuntimeException("boom"));
        assertThrows(CartOperationException.class, () -> service.calculateCartItemCount(2L));
    }

    @Test
    void updateQuantity_shouldRejectNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(1L, 0));
    }

    @Test
    void updateQuantity_shouldValidateStockAndSave() {
        Product p = product(5L, "10.00", 10);
        CartItem item = item(1L, 1, "10.00", p);
        when(selfProxy.getCartItemById(1L)).thenReturn(item);
        when(selfProxy.saveCartItem(item)).thenReturn(item);

        CartItem result = service.updateQuantity(1L, 4);

        assertEquals(4, result.getQuantity());
        verify(inventoryService).ensureAvailableStock(5L, 4);
    }

    @Test
    void updateQuantity_shouldPropagateStockError() {
        Product p = product(5L, "10.00", 1);
        CartItem item = item(1L, 1, "10.00", p);
        when(selfProxy.getCartItemById(1L)).thenReturn(item);
        doThrow(new RuntimeException("out of stock"))
                .when(inventoryService).ensureAvailableStock(eq(5L), eq(9));

        assertThrows(RuntimeException.class, () -> service.updateQuantity(1L, 9));
        verify(selfProxy, never()).saveCartItem(any());
    }

    @Test
    void updateUnitPrice_shouldRejectNullOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUnitPrice(1L, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateUnitPrice(1L, new BigDecimal("-1")));
    }

    @Test
    void updateUnitPrice_shouldUpdateAndSave() {
        CartItem item = item(1L, 1, "10.00", product(5L, "10.00", 3));
        when(selfProxy.getCartItemById(1L)).thenReturn(item);
        when(selfProxy.saveCartItem(item)).thenReturn(item);

        CartItem result = service.updateUnitPrice(1L, new BigDecimal("15.00"));

        assertEquals(new BigDecimal("15.00"), result.getUnitPrice());
    }

    @Test
    void findExpiredItems_shouldReturnEmpty() {
        assertTrue(service.findExpiredItems(2L, 30).isEmpty());
    }

    @Test
    void refreshItemPrices_shouldUpdateChangedPrices() {
        Product p = product(5L, "12.00", 3);
        CartItem item = item(1L, 1, "10.00", p);
        when(selfProxy.getCartItemsByCartId(2L)).thenReturn(List.of(item));
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(selfProxy.saveCartItem(item)).thenReturn(item);

        service.refreshItemPrices(2L);

        assertEquals(new BigDecimal("12.00"), item.getUnitPrice());
        verify(selfProxy).saveCartItem(item);
    }

    @Test
    void refreshItemPrices_shouldSkipWhenPriceUnchanged() {
        Product p = product(5L, "10.00", 3);
        CartItem item = item(1L, 1, "10.00", p);
        when(selfProxy.getCartItemsByCartId(2L)).thenReturn(List.of(item));
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));

        service.refreshItemPrices(2L);

        verify(selfProxy, never()).saveCartItem(any());
    }

    @Test
    void refreshItemPrices_shouldWrapError() {
        when(selfProxy.getCartItemsByCartId(2L)).thenThrow(new RuntimeException("boom"));
        assertThrows(CartOperationException.class, () -> service.refreshItemPrices(2L));
    }

    @Test
    void findItemsWithInsufficientStock_shouldFilter() {
        CartItem ok = item(1L, 1, "10.00", product(5L, "10.00", 5));
        CartItem low = item(2L, 8, "10.00", product(6L, "10.00", 3));
        when(selfProxy.getCartItemsByCartId(2L)).thenReturn(List.of(ok, low));

        List<CartItem> result = service.findItemsWithInsufficientStock(2L);

        assertEquals(1, result.size());
        assertSame(low, result.get(0));
    }

    @Test
    void findItemsWithInsufficientStock_shouldWrapError() {
        when(selfProxy.getCartItemsByCartId(2L)).thenThrow(new RuntimeException("boom"));
        assertThrows(CartOperationException.class,
                () -> service.findItemsWithInsufficientStock(2L));
    }

    @Test
    void getCartItemsByCartId_paged_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CartItem> page = new PageImpl<>(
                List.of(item(1L, 1, "10.00", product(5L, "10.00", 3))), pageable, 1);
        when(cartItemRepository.findByCartId(2L, pageable)).thenReturn(page);
        assertEquals(1, service.getCartItemsByCartId(2L, pageable).getTotalElements());
    }

    @Test
    void getCartItemsByCartId_paged_shouldWrapError() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cartItemRepository.findByCartId(2L, pageable)).thenThrow(new RuntimeException("db"));
        assertThrows(CartOperationException.class,
                () -> service.getCartItemsByCartId(2L, pageable));
    }

    @Test
    void saveAllCartItems_shouldPersistBatch() {
        List<CartItem> items = List.of(
                item(null, 1, "10.00", product(5L, "10.00", 3)),
                item(null, 2, "5.00", product(6L, "5.00", 3)));
        when(cartItemRepository.saveAll(items)).thenReturn(items);
        assertEquals(2, service.saveAllCartItems(items).size());
    }

    @Test
    void saveAllCartItems_shouldWrapValidationError() {
        List<CartItem> items = List.of(item(null, 0, "10.00", product(5L, "10.00", 3)));
        assertThrows(CartOperationException.class, () -> service.saveAllCartItems(items));
        verify(cartItemRepository, never()).saveAll(any());
    }
}
