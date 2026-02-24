package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.dto.ProductDto;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllProducts_ShouldMapRepositoryEntitiesToDtoPage() {
        Product product = new Product(1L, "Phone", "Flagship", new BigDecimal("999.99"), "img", "Electronics", 10);
        when(productRepository.findAll(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(product)));

        var result = productService.getAllProducts(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Phone", result.getContent().get(0).getName());
        assertEquals(new BigDecimal("999.99"), result.getContent().get(0).getPrice());
    }

    @Test
    void getProductById_ShouldReturnDto_WhenProductExists() {
        Product product = new Product(2L, "Keyboard", "Mechanical", new BigDecimal("120.00"), "img2", "Accessories", 5);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        ProductDto result = productService.getProductById(2L);

        assertEquals(2L, result.getId());
        assertEquals("Keyboard", result.getName());
    }

    @Test
    void getProductById_ShouldThrow_WhenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void createProduct_ShouldPersistAndReturnDto() {
        ProductDto request = new ProductDto(null, "Mouse", "Wireless", new BigDecimal("49.90"), "img3", "Accessories");
        request.setStock(20);
        Product saved = new Product(3L, "Mouse", "Wireless", new BigDecimal("49.90"), "img3", "Accessories", 20);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDto result = productService.createProduct(request);

        assertEquals(3L, result.getId());
        assertEquals("Mouse", result.getName());
        verify(inventoryService).initializeStock(3L, 20);
    }

    @Test
    void updateProduct_ShouldUpdateFieldsAndReturnDto() {
        Product existing = new Product(4L, "Old", "Old desc", new BigDecimal("10.00"), "old", "OldCat", 1);
        Product requestApplied = new Product(4L, "New", "New desc", new BigDecimal("20.00"), "new", "NewCat", 1);

        when(productRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(requestApplied);

        ProductDto request = new ProductDto(null, "New", "New desc", new BigDecimal("20.00"), "new", "NewCat");
        request.setStock(7);
        ProductDto result = productService.updateProduct(4L, request);

        assertEquals("New", result.getName());
        assertEquals("NewCat", result.getCategory());
        verify(inventoryService).setStock(4L, 7);
    }

    @Test
    void deleteProduct_ShouldDelete_WhenProductExists() {
        Product existing = new Product(5L, "Item", "Desc", new BigDecimal("15.00"), "img", "Cat", 2);
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(5L);

        verify(productRepository).delete(existing);
    }

    @Test
    void updateProduct_ShouldThrow_WhenProductMissing() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        ProductDto request = new ProductDto(null, "New", "New desc", new BigDecimal("20.00"), "new", "NewCat");
        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(404L, request));
    }

    @Test
    void deleteProduct_ShouldThrow_WhenProductMissing() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(404L));
    }

    @Test
    void searchProducts_ShouldReturnPagedDtoResult() {
        Product product = new Product(10L, "Amp", "Tube amp", new BigDecimal("799.00"), "img", "Amplifier", 4);
        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(product)));

        var result = productService.searchProducts("Amplifier", null, "tube", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Amp", result.getContent().get(0).getName());
    }

    @Test
    void createProduct_ShouldUseAuthenticatedUserIdInAudit() {
        User user = User.builder()
                .id(77L)
                .username("admin")
                .password("pw")
                .email("admin@test.com")
                .firstName("A")
                .lastName("B")
                .roles(java.util.Set.of(Role.ROLE_ADMIN))
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        ProductDto request = new ProductDto(null, "Mouse", "Wireless", new BigDecimal("49.90"), "img3", "Accessories");
        request.setStock(20);
        Product saved = new Product(3L, "Mouse", "Wireless", new BigDecimal("49.90"), "img3", "Accessories", 20);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        productService.createProduct(request);

        verify(auditService).logSystemEvent(eq(77L), eq("admin"), any(), any(), any());
        verify(auditService, never()).logSystemEvent(eq((Long) null), eq("admin"), any(), any(), any());
    }
}
