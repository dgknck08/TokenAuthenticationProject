package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.model.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByCartId(Long cartId);
    
    Page<CartItem> findByCartId(Long cartId, Pageable pageable);
    
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    // Count and statistics
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    long countByCartId(@Param("cartId") Long cartId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") Long cartId);
    
    @Query("SELECT SUM(ci.unitPrice * ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId")
    BigDecimal sumTotalByCartId(@Param("cartId") Long cartId);
    
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product p WHERE ci.cart.id = :cartId AND p.stock < ci.quantity")
    List<CartItem> findItemsWithInsufficientStock(@Param("cartId") Long cartId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.unitPrice != ci.product.price")
    List<CartItem> findItemsWithOutdatedPrices(@Param("cartId") Long cartId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.createdAt < :beforeDate")
    List<CartItem> findOldItems(@Param("cartId") Long cartId, @Param("beforeDate") LocalDateTime beforeDate);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.product.id = :productId")
    List<CartItem> findByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
    List<CartItem> findByUserId(@Param("userId") Long userId);
    
    boolean existsByCartIdAndProductId(Long cartId, Long productId);
}