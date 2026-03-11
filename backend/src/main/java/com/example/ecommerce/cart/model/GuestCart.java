package com.example.ecommerce.cart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuestCart implements Serializable {
    private String sessionId;
    private Map<Long, GuestCartItem> items = new HashMap<>();
    private Long createdAt;
    private Long updatedAt;
    
    public void addItem(Long productId, int quantity, BigDecimal unitPrice, String productName) {
        GuestCartItem existingItem = items.get(productId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            GuestCartItem newItem = new GuestCartItem();
            newItem.setProductId(productId);
            newItem.setProductName(productName);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(unitPrice);
            items.put(productId, newItem);
        }
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void updateItemQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            items.remove(productId);
        } else {
            GuestCartItem item = items.get(productId);
            if (item != null) {
                item.setQuantity(quantity);
            }
        }
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void removeItem(Long productId) {
        items.remove(productId);
        this.updatedAt = System.currentTimeMillis();
    }
    
    @JsonIgnore
    public BigDecimal getTotalAmount() {
        return items.values().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @JsonIgnore
    public int getTotalItems() {
        return items.values().stream()
                .mapToInt(GuestCartItem::getQuantity)
                .sum();
    }
}
