package com.example.ecommerce.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ecommerce.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
