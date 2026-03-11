package com.example.ecommerce.wishlist.repository;

import com.example.ecommerce.wishlist.model.WishlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    @EntityGraph(attributePaths = "product")
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "product")
    Optional<WishlistItem> findByUserIdAndProduct_Id(Long userId, Long productId);

    long deleteByUserIdAndProduct_Id(Long userId, Long productId);
}
