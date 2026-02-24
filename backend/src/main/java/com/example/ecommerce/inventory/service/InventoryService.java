package com.example.ecommerce.inventory.service;

import com.example.ecommerce.cart.exception.InsufficientStockException;
import com.example.ecommerce.inventory.model.InventoryItem;
import com.example.ecommerce.inventory.repository.InventoryRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public void initializeStock(Long productId, int initialStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + productId));

        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseGet(InventoryItem::new);
        item.setProduct(product);
        item.setAvailableStock(Math.max(initialStock, 0));
        item.setReservedStock(0);
        item.setReorderLevel(5);
        item.setUpdatedAt(Instant.now());
        inventoryRepository.save(item);
    }

    @Transactional(readOnly = true)
    public int getAvailableStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(InventoryItem::getAvailableStock)
                .orElseGet(() -> productRepository.findById(productId).map(Product::getStock).orElse(0));
    }

    public void setStock(Long productId, int newStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + productId));
        product.setStock(Math.max(newStock, 0));

        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseGet(InventoryItem::new);
        item.setProduct(product);
        item.setAvailableStock(Math.max(newStock, 0));
        item.setUpdatedAt(Instant.now());
        if (item.getReorderLevel() == 0) {
            item.setReorderLevel(5);
        }
        inventoryRepository.save(item);
    }

    public void decreaseStockWithOptimisticLock(Long productId, int quantity) {
        adjustStock(productId, -quantity);
    }

    public void increaseStockWithOptimisticLock(Long productId, int quantity) {
        adjustStock(productId, quantity);
    }

    private void adjustStock(Long productId, int delta) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + productId));

        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createInventoryItem(product));

        int current = item.getAvailableStock();
        int updated = current + delta;
        if (updated < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + ". Available: " + current + ", Requested: " + Math.abs(delta)
            );
        }

        item.setAvailableStock(updated);
        item.setUpdatedAt(Instant.now());
        if (item.getReorderLevel() == 0) {
            item.setReorderLevel(5);
        }
        product.setStock(updated);
        inventoryRepository.saveAndFlush(item);
    }

    private InventoryItem createInventoryItem(Product product) {
        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setAvailableStock(Math.max(product.getStock(), 0));
        item.setReservedStock(0);
        item.setReorderLevel(5);
        item.setUpdatedAt(Instant.now());
        return item;
    }

    @Transactional(readOnly = true)
    public void ensureAvailableStock(Long productId, int requestedQuantity) {
        int available = getAvailableStock(productId);
        if (requestedQuantity > available) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + ". Available: " + available + ", Requested: " + requestedQuantity
            );
        }
    }
}
