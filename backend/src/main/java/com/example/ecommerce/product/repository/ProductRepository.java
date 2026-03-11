package com.example.ecommerce.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.example.ecommerce.product.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategoryIgnoreCase(String category);
    List<Product> findByBrandIgnoreCase(String brand);
    Optional<Product> findBySku(String sku);

    @Modifying
    @Query("delete from Product p where p.sku in :skus")
    @Transactional
    long deleteBySkuIn(@Param("skus") List<String> skus);

    @Query(
            value = """
                    SELECT p.*
                    FROM product p
                    WHERE (:category IS NULL OR lower(p.category) = lower(:category))
                      AND (:brand IS NULL OR lower(p.brand) = lower(:brand))
                      AND (
                            :query IS NULL
                            OR p.search_vector @@ websearch_to_tsquery('simple', :query)
                            OR similarity(COALESCE(p.name, ''), :query) >= 0.20
                            OR similarity(COALESCE(p.description, ''), :query) >= 0.15
                          )
                    ORDER BY
                      CASE
                        WHEN :query IS NULL THEN 0
                        ELSE (
                          ts_rank_cd(p.search_vector, websearch_to_tsquery('simple', :query)) * 0.75
                          + GREATEST(
                              similarity(COALESCE(p.name, ''), :query),
                              similarity(COALESCE(p.description, ''), :query)
                            ) * 0.25
                        )
                      END DESC,
                      p.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM product p
                    WHERE (:category IS NULL OR lower(p.category) = lower(:category))
                      AND (:brand IS NULL OR lower(p.brand) = lower(:brand))
                      AND (
                            :query IS NULL
                            OR p.search_vector @@ websearch_to_tsquery('simple', :query)
                            OR similarity(COALESCE(p.name, ''), :query) >= 0.20
                            OR similarity(COALESCE(p.description, ''), :query) >= 0.15
                          )
                    """,
            nativeQuery = true
    )
    Page<Product> searchProductsAdvanced(@Param("category") String category,
                                         @Param("brand") String brand,
                                         @Param("query") String query,
                                         Pageable pageable);
}
