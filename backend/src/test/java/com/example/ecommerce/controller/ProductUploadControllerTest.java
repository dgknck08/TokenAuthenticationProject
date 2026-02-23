package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.example.ecommerce.product.controller.ProductUploadController;
import com.example.ecommerce.product.dto.ProductImageUploadResponse;
import com.example.ecommerce.product.service.ProductImageUploadService;

@ExtendWith(MockitoExtension.class)
class ProductUploadControllerTest {

    @Mock
    private ProductImageUploadService productImageUploadService;

    @InjectMocks
    private ProductUploadController controller;

    @Test
    void uploadProductImage_returnsOkWithResponseBody() {
        MockMultipartFile file = new MockMultipartFile("file", "shoe.png", "image/png", new byte[] {1, 2, 3});
        ProductImageUploadResponse uploadResponse =
                new ProductImageUploadResponse("https://cdn/img.png", "products/img", "png", 100, 80, 123L);
        when(productImageUploadService.uploadProductImage(file)).thenReturn(uploadResponse);

        ResponseEntity<ProductImageUploadResponse> response = controller.uploadProductImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://cdn/img.png", response.getBody().getUrl());
        verify(productImageUploadService).uploadProductImage(file);
    }
}
