package com.example.ecommerce.review.repository;

import com.example.ecommerce.review.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    boolean existsByProduct_IdAndUserId(Long productId, Long userId);

    Page<ProductReview> findByProduct_IdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}
