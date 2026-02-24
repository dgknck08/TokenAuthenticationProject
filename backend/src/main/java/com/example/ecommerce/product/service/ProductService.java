package com.example.ecommerce.product.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.security.CustomUserDetails;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.inventory.service.InventoryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.ecommerce.product.dto.ProductDto;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.mapper.ProductMapper;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    // Constructor injection
    public ProductService(ProductRepository productRepository, InventoryService inventoryService, AuditService auditService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductMapper::toDto);
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        return ProductMapper.toDto(product);
    }

    @Cacheable(value = "productSearch", key = "T(String).format('%s|%s|%s|%s|%s|%s', #category, #brand, #query, #pageable.pageNumber, #pageable.pageSize, #pageable.sort)")
    public Page<ProductDto> searchProducts(String category, String brand, String query, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);

        if (hasText(category)) {
            String normalizedCategory = category.trim().toLowerCase();
            spec = spec.and((root, cq, cb) -> cb.equal(cb.lower(root.get("category")), normalizedCategory));
        }

        if (hasText(brand)) {
            String normalizedBrand = brand.trim().toLowerCase();
            spec = spec.and((root, cq, cb) -> cb.equal(cb.lower(root.get("brand")), normalizedBrand));
        }

        if (hasText(query)) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        return productRepository.findAll(spec, pageable).map(ProductMapper::toDto);
    }

    @CacheEvict(value = "productSearch", allEntries = true)
    public ProductDto createProduct(ProductDto productDto) {
        Product product = ProductMapper.toEntity(productDto);
        normalizeProductFields(product);
        Product savedProduct = productRepository.save(product);
        inventoryService.initializeStock(savedProduct.getId(), productDto.getStock());
        Map<String, Object> details = new HashMap<>();
        details.put("productId", savedProduct.getId());
        details.put("sku", savedProduct.getSku());
        details.put("name", savedProduct.getName());
        auditService.logSystemEvent(getCurrentUserId(), getCurrentUsername(), AuditLog.AuditAction.ADMIN_PRODUCT_CREATED,
                "Product created", details);
        return ProductMapper.toDto(savedProduct);
    }

    @CacheEvict(value = "productSearch", allEntries = true)
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));

        existing.setName(productDto.getName());
        existing.setDescription(productDto.getDescription());
        existing.setPrice(productDto.getPrice());
        existing.setImageUrl(productDto.getImageUrl());
        existing.setCategory(productDto.getCategory());
        existing.setBrand(productDto.getBrand());
        existing.setSku(productDto.getSku());
        existing.setColor(productDto.getColor());
        existing.setSize(productDto.getSize());
        existing.setAttributesJson(productDto.getAttributesJson());
        existing.setStock(productDto.getStock());
        normalizeProductFields(existing);

        Product updatedProduct = productRepository.save(existing);
        inventoryService.setStock(updatedProduct.getId(), productDto.getStock());
        Map<String, Object> details = new HashMap<>();
        details.put("productId", updatedProduct.getId());
        details.put("sku", updatedProduct.getSku());
        auditService.logSystemEvent(getCurrentUserId(), getCurrentUsername(), AuditLog.AuditAction.ADMIN_PRODUCT_UPDATED,
                "Product updated", details);
        return ProductMapper.toDto(updatedProduct);
    }

    @CacheEvict(value = "productSearch", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        productRepository.delete(product);
        Map<String, Object> details = new HashMap<>();
        details.put("productId", id);
        details.put("sku", product.getSku());
        auditService.logSystemEvent(getCurrentUserId(), getCurrentUsername(), AuditLog.AuditAction.ADMIN_PRODUCT_DELETED,
                "Product deleted", details);
    }

    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(ProductMapper::toDto)
                .toList();
    }

    public List<ProductDto> getProductsByBrand(String brand) {
        return productRepository.findByBrandIgnoreCase(brand).stream()
                .map(ProductMapper::toDto)
                .toList();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails && customUserDetails.getUser() != null) {
            return customUserDetails.getUser().getId();
        }
        return null;
    }

    private void normalizeProductFields(Product product) {
        product.setName(trimOrNull(product.getName()));
        product.setDescription(trimOrNull(product.getDescription()));
        product.setCategory(trimOrNull(product.getCategory()));
        product.setBrand(trimOrNull(product.getBrand()));
        product.setSku(trimOrNull(product.getSku()));
        product.setColor(trimOrNull(product.getColor()));
        product.setSize(trimOrNull(product.getSize()));
        product.setAttributesJson(trimOrNull(product.getAttributesJson()));
        product.setImageUrl(normalizeImageUrl(product.getImageUrl()));
    }

    private String normalizeImageUrl(String imageUrl) {
        String normalized = trimOrNull(imageUrl);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/products/")) {
            return normalized;
        }
        return null;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
