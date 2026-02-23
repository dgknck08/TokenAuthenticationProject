package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.ecommerce.product.controller.ProductController;
import com.example.ecommerce.product.dto.ProductDto;
import com.example.ecommerce.product.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void getAllProducts_ShouldReturnOkWithBody() {
        ProductDto dto = new ProductDto(1L, "Phone", "Flagship", new BigDecimal("999.99"), "img", "Electronics");
        when(productService.getAllProducts()).thenReturn(List.of(dto));

        ResponseEntity<List<ProductDto>> response = productController.getAllProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getProductById_ShouldReturnOkWithProduct() {
        ProductDto dto = new ProductDto(2L, "Mouse", "Wireless", new BigDecimal("49.90"), "img", "Accessories");
        when(productService.getProductById(2L)).thenReturn(dto);

        ResponseEntity<ProductDto> response = productController.getProductById(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Mouse", response.getBody().getName());
    }

    @Test
    void getProductsByCategory_ShouldReturnOkWithBody() {
        ProductDto dto = new ProductDto(11L, "Shirt", "Cotton", new BigDecimal("29.90"), "img", "Apparel");
        when(productService.getProductsByCategory("Apparel")).thenReturn(List.of(dto));

        ResponseEntity<List<ProductDto>> response = productController.getProductsByCategory("Apparel");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Shirt", response.getBody().get(0).getName());
    }

    @Test
    void getProductsByBrand_ShouldReturnOkWithBody() {
        ProductDto dto = new ProductDto(12L, "Sneaker", "Sport", new BigDecimal("59.90"), "img", "Shoes");
        dto.setBrand("Acme");
        when(productService.getProductsByBrand("Acme")).thenReturn(List.of(dto));

        ResponseEntity<List<ProductDto>> response = productController.getProductsByBrand("Acme");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Sneaker", response.getBody().get(0).getName());
    }

    @Test
    void createProduct_ShouldReturnCreatedStatus() {
        ProductDto request = new ProductDto(null, "Keyboard", "Mechanical", new BigDecimal("100.00"), "img", "Accessories");
        ProductDto created = new ProductDto(3L, "Keyboard", "Mechanical", new BigDecimal("100.00"), "img", "Accessories");
        when(productService.createProduct(request)).thenReturn(created);

        ResponseEntity<ProductDto> response = productController.createProduct(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(3L, response.getBody().getId());
    }

    @Test
    void updateProduct_ShouldReturnOkWithUpdatedProduct() {
        ProductDto request = new ProductDto(null, "Updated", "Updated", new BigDecimal("10.00"), "img", "Cat");
        ProductDto updated = new ProductDto(4L, "Updated", "Updated", new BigDecimal("10.00"), "img", "Cat");
        when(productService.updateProduct(4L, request)).thenReturn(updated);

        ResponseEntity<ProductDto> response = productController.updateProduct(4L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4L, response.getBody().getId());
    }

    @Test
    void deleteProduct_ShouldReturnNoContent() {
        ResponseEntity<Void> response = productController.deleteProduct(5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).deleteProduct(5L);
    }
}
