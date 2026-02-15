package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.dto.ProductImageUploadResponse;
import com.example.ecommerce.product.service.ProductImageUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
public class ProductUploadController {

    private final ProductImageUploadService productImageUploadService;

    public ProductUploadController(ProductImageUploadService productImageUploadService) {
        this.productImageUploadService = productImageUploadService;
    }

    @PostMapping(value = "/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<ProductImageUploadResponse> uploadProductImage(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(productImageUploadService.uploadProductImage(file));
    }
}
