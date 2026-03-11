package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByOrder_Id(Long orderId);

    @EntityGraph(attributePaths = "order")
    Page<ReturnRequest> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    @EntityGraph(attributePaths = "order")
    Page<ReturnRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
