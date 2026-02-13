package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.model.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.exception.CartItemNotFoundException;
import com.example.ecommerce.cart.exception.CartOperationException;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartItemServiceImpl implements CartItemService {
    
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    // ================ Basic CRUD Operations ================
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cartItems", key = "#id")
    public CartItem getCartItemById(Long id) {
        log.debug("Getting cart item by ID: {}", id);
        
        return cartItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cart item not found with ID: {}", id);
                    return new CartItemNotFoundException("Cart item not found with ID: " + id);
                });
    }
    
    @Override
    @CacheEvict(value = "cartItems", key = "#item.id")
    public CartItem saveCartItem(CartItem item) {
        log.debug("Saving cart item: {}", item.getId());
        
        try {
            validateCartItem(item);
            updateItemPrice(item);
            
            CartItem savedItem = cartItemRepository.save(item);
            log.info("Successfully saved cart item: {}", savedItem.getId());
            
            return savedItem;
        } catch (Exception e) {
            log.error("Error saving cart item: {}", e.getMessage(), e);
            throw new CartOperationException("Failed to save cart item", e);
        }
    }
    
    @Override
    @CacheEvict(value = "cartItems", key = "#id")
    public void deleteCartItem(Long id) {
        log.debug("Deleting cart item with ID: {}", id);
        
        if (!cartItemRepository.existsById(id)) {
            throw new CartItemNotFoundException("Cart item not found with ID: " + id);
        }
        
        try {
            cartItemRepository.deleteById(id);
            log.info("Successfully deleted cart item: {}", id);
        } catch (Exception e) {
            log.error("Error deleting cart item: {}", e.getMessage(), e);
            throw new CartOperationException("Failed to delete cart item", e);
        }
    }

    // ================ Query Operations ================
    
    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsByCartId(Long cartId) {
        log.debug("Getting cart items for cart ID: {}", cartId);
        
        try {
            List<CartItem> items = cartItemRepository.findByCartId(cartId);
            log.debug("Found {} cart items for cart ID: {}", items.size(), cartId);
            return items;
        } catch (Exception e) {
            log.error("Error getting cart items for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to get cart items", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId) {
        log.debug("Finding cart item for cart ID: {} and product ID: {}", cartId, productId);
        
        try {
            Optional<CartItem> item = cartItemRepository.findByCartIdAndProductId(cartId, productId);
            log.debug("Cart item {} found for cart ID: {} and product ID: {}", 
                    item.isPresent() ? "found" : "not found", cartId, productId);
            return item;
        } catch (Exception e) {
            log.error("Error finding cart item for cart ID: {} and product ID: {}", cartId, productId, e);
            throw new CartOperationException("Failed to find cart item", e);
        }
    }

    // ================ Bulk Operations ================
    
    @Override
    public void deleteByCartIdAndProductId(Long cartId, Long productId) {
        log.debug("Deleting cart item for cart ID: {} and product ID: {}", cartId, productId);
        
        try {
            Optional<CartItem> existingItem = findByCartIdAndProductId(cartId, productId);
            if (existingItem.isEmpty()) {
                log.warn("No cart item found to delete for cart ID: {} and product ID: {}", cartId, productId);
                throw new CartItemNotFoundException("Cart item not found for deletion");
            }
            
            cartItemRepository.deleteByCartIdAndProductId(cartId, productId);
            log.info("Successfully deleted cart item for cart ID: {} and product ID: {}", cartId, productId);
        } catch (CartItemNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting cart item for cart ID: {} and product ID: {}", cartId, productId, e);
            throw new CartOperationException("Failed to delete cart item", e);
        }
    }
    
    @Override
    @CacheEvict(value = "cartItems", allEntries = true)
    public void deleteByCartId(Long cartId) {
        log.debug("Deleting all cart items for cart ID: {}", cartId);
        
        try {
            long deletedCount = cartItemRepository.countByCartId(cartId);
            cartItemRepository.deleteByCartId(cartId);
            log.info("Successfully deleted {} cart items for cart ID: {}", deletedCount, cartId);
        } catch (Exception e) {
            log.error("Error deleting cart items for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to delete cart items", e);
        }
    }

    // ================ Statistics and Utility Operations ================
    
    @Override
    @Transactional(readOnly = true)
    public long countByCartId(Long cartId) {
        log.debug("Counting cart items for cart ID: {}", cartId);
        
        try {
            long count = cartItemRepository.countByCartId(cartId);
            log.debug("Found {} cart items for cart ID: {}", count, cartId);
            return count;
        } catch (Exception e) {
            log.error("Error counting cart items for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to count cart items", e);
        }
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateCartTotal(Long cartId) {
        log.debug("Calculating total for cart ID: {}", cartId);
        
        try {
            List<CartItem> items = getCartItemsByCartId(cartId);
            BigDecimal total = items.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            log.debug("Calculated total {} for cart ID: {}", total, cartId);
            return total;
        } catch (Exception e) {
            log.error("Error calculating cart total for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to calculate cart total", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public int calculateCartItemCount(Long cartId) {
        log.debug("Calculating item count for cart ID: {}", cartId);
        
        try {
            List<CartItem> items = getCartItemsByCartId(cartId);
            int totalItems = items.stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            
            log.debug("Calculated {} total items for cart ID: {}", totalItems, cartId);
            return totalItems;
        } catch (Exception e) {
            log.error("Error calculating cart item count for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to calculate cart item count", e);
        }
    }

    // ================ Advanced Operations ================
    
    @Override
    public CartItem updateQuantity(Long cartItemId, int newQuantity) {
        log.debug("Updating quantity to {} for cart item ID: {}", newQuantity, cartItemId);
        
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        CartItem item = getCartItemById(cartItemId);
        
        // Validate stock if needed
        validateStock(item.getProduct(), newQuantity);
        
        item.setQuantity(newQuantity);
        CartItem updatedItem = saveCartItem(item);
        
        log.info("Successfully updated quantity to {} for cart item ID: {}", newQuantity, cartItemId);
        return updatedItem;
    }
    
    @Override
    public CartItem updateUnitPrice(Long cartItemId, BigDecimal newPrice) {
        log.debug("Updating unit price to {} for cart item ID: {}", newPrice, cartItemId);
        
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be null or negative");
        }
        
        CartItem item = getCartItemById(cartItemId);
        item.setUnitPrice(newPrice);
        CartItem updatedItem = saveCartItem(item);
        
        log.info("Successfully updated unit price to {} for cart item ID: {}", newPrice, cartItemId);
        return updatedItem;
    }
    
    @Transactional(readOnly = true)
    public List<CartItem> findExpiredItems(Long cartId, int maxAgeInDays) {
        log.debug("Finding expired items (older than {} days) for cart ID: {}", maxAgeInDays, cartId);
        
        // Bu method için CartItem entity'sinde createdAt veya updatedAt field'ı olmalı
        // Şimdilik empty list döndürüyoruz
        return List.of();
    }
    
    public List<CartItem> refreshItemPrices(Long cartId) {
        log.debug("Refreshing item prices for cart ID: {}", cartId);
        
        try {
            List<CartItem> items = getCartItemsByCartId(cartId);
            
            for (CartItem item : items) {
                Product currentProduct = productRepository.findById(item.getProduct().getId())
                        .orElse(null);
                
                if (currentProduct != null && 
                    !item.getUnitPrice().equals(currentProduct.getPrice())) {
                    
                    log.debug("Updating price for item {} from {} to {}", 
                            item.getId(), item.getUnitPrice(), currentProduct.getPrice());
                    
                    item.setUnitPrice(currentProduct.getPrice());
                    saveCartItem(item);
                }
            }
            
            log.info("Successfully refreshed prices for {} items in cart ID: {}", items.size(), cartId);
            return items;
        } catch (Exception e) {
            log.error("Error refreshing item prices for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to refresh item prices", e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<CartItem> findItemsWithInsufficientStock(Long cartId) {
        log.debug("Finding items with insufficient stock for cart ID: {}", cartId);
        
        try {
            List<CartItem> items = getCartItemsByCartId(cartId);
            
            List<CartItem> insufficientStockItems = items.stream()
                    .filter(item -> {
                        Product product = item.getProduct();
                        return product.getStock() < item.getQuantity();
                    })
                    .toList();
            
            log.debug("Found {} items with insufficient stock for cart ID: {}", 
                    insufficientStockItems.size(), cartId);
            
            return insufficientStockItems;
        } catch (Exception e) {
            log.error("Error finding items with insufficient stock for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to find items with insufficient stock", e);
        }
    }

    
    private void validateCartItem(CartItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }
        
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative");
        }
        
        if (item.getProduct() == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
    }
    
    private void updateItemPrice(CartItem item) {
        // Eğer fiyat güncellemesi gerekiyorsa
        if (item.getId() != null) { // Existing item
            Product currentProduct = productRepository.findById(item.getProduct().getId())
                    .orElse(null);
            
            if (currentProduct != null && 
                !item.getUnitPrice().equals(currentProduct.getPrice())) {
                log.debug("Price mismatch detected. Updating from {} to {} for item {}", 
                        item.getUnitPrice(), currentProduct.getPrice(), item.getId());
                // İsteğe bağlı: Fiyatı otomatik güncelle veya exception fırlat
                // item.setUnitPrice(currentProduct.getPrice());
            }
        }
    }
    
    private void validateStock(Product product, int requestedQuantity) {
        inventoryService.ensureAvailableStock(product.getId(), requestedQuantity);
    }
    
    // ================ Pagination Support ================
    
    @Override
    @Transactional(readOnly = true)
    public Page<CartItem> getCartItemsByCartId(Long cartId, Pageable pageable) {
        log.debug("Getting cart items for cart ID: {} with pagination", cartId);
        
        try {
            Page<CartItem> itemsPage = cartItemRepository.findByCartId(cartId, pageable);
            log.debug("Found {} cart items (page {} of {}) for cart ID: {}", 
                    itemsPage.getNumberOfElements(), 
                    itemsPage.getNumber() + 1, 
                    itemsPage.getTotalPages(), 
                    cartId);
            return itemsPage;
        } catch (Exception e) {
            log.error("Error getting paginated cart items for cart ID: {}", cartId, e);
            throw new CartOperationException("Failed to get paginated cart items", e);
        }
    }
    
    // ================ Batch Operations ================
    
    @Override
    public List<CartItem> saveAllCartItems(List<CartItem> items) {
        log.debug("Saving {} cart items in batch", items.size());
        
        try {
            items.forEach(this::validateCartItem);
            
            List<CartItem> savedItems = cartItemRepository.saveAll(items);
            log.info("Successfully saved {} cart items in batch", savedItems.size());
            
            return savedItems;
        } catch (Exception e) {
            log.error("Error saving cart items in batch", e);
            throw new CartOperationException("Failed to save cart items in batch", e);
        }
    }
}
