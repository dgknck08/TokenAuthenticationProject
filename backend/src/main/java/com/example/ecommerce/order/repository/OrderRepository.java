package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
