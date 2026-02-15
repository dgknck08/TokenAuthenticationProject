package com.example.ecommerce.product.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.inventory.service.InventoryService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<ProductDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        return ProductMapper.toDto(product);
    }

    public ProductDto createProduct(ProductDto productDto) {
        Product product = ProductMapper.toEntity(productDto);
        normalizeProductFields(product);
        Product savedProduct = productRepository.save(product);
        inventoryService.initializeStock(savedProduct.getId(), productDto.getStock());
        Map<String, Object> details = Map.of(
                "productId", savedProduct.getId(),
                "sku", String.valueOf(savedProduct.getSku()),
                "name", savedProduct.getName()
        );
        auditService.logSystemEvent(getCurrentUserId(), getCurrentUsername(), AuditLog.AuditAction.ADMIN_PRODUCT_CREATED,
                "Product created", details);
        return ProductMapper.toDto(savedProduct);
    }

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
        auditService.logSystemEvent(getCurrentUserId(), getCurrentUsername(), AuditLog.AuditAction.ADMIN_PRODUCT_UPDATED,
                "Product updated", Map.of("productId", updatedProduct.getId(), "sku", String.valueOf(updatedProduct.getSku())));
        return ProductMapper.toDto(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        productRepository.delete(product);
        auditService.logSystemEvent(getCurrentUserId(), getCurrentUsername(), AuditLog.AuditAction.ADMIN_PRODUCT_DELETED,
                "Product deleted", Map.of("productId", id, "sku", String.valueOf(product.getSku())));
    }

    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> getProductsByBrand(String brand) {
        return productRepository.findByBrandIgnoreCase(brand).stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private Long getCurrentUserId() {
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
}
