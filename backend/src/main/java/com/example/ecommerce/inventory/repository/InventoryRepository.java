package com.example.ecommerce.inventory.repository;

import com.example.ecommerce.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByProductId(Long productId);

    List<InventoryItem> findByProductIdIn(Collection<Long> productIds);
}
