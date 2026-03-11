package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<Order> findByPaymentConversationId(String paymentConversationId);
    Optional<Order> findByPaymentToken(String paymentToken);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.id IN :orderIds
            """)
    List<Order> findAllWithItemsAndProductByIdIn(@Param("orderIds") List<Long> orderIds);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdWithItemsAndProduct(@Param("orderId") Long orderId);
}
