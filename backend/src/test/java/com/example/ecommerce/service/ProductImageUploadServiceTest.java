package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.ecommerce.config.CloudinaryProperties;
import com.example.ecommerce.product.dto.ProductImageUploadResponse;
import com.example.ecommerce.product.service.ProductImageUploadService;

@ExtendWith(MockitoExtension.class)
class ProductImageUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryProperties properties;
    private ProductImageUploadService service;

    @BeforeEach
    void setUp() {
        properties = new CloudinaryProperties();
        properties.setCloudName("demo-cloud");
        properties.setApiKey("demo-key");
        properties.setApiSecret("demo-secret");
        properties.setFolder("ecommerce/products");
        service = new ProductImageUploadService(cloudinary, properties);
    }

    @Test
    void uploadProductImage_whenValidImage_returnsMappedResponse() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "shoe.png", "image/png", new byte[] {1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://cdn.example.com/shoe.png",
                "public_id", "ecommerce/products/shoe-123",
                "format", "png",
                "width", 1200,
                "height", 800,
                "bytes", 123456L
        ));

        ProductImageUploadResponse response = service.uploadProductImage(file);

        assertEquals("https://cdn.example.com/shoe.png", response.getUrl());
        assertEquals("ecommerce/products/shoe-123", response.getPublicId());
        assertEquals("png", response.getFormat());
        assertEquals(1200, response.getWidth());
        assertEquals(800, response.getHeight());
        assertEquals(123456L, response.getBytes());
    }

    @Test
    void uploadProductImage_whenContentTypeInvalid_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[] {1});
        assertThrows(IllegalArgumentException.class, () -> service.uploadProductImage(file));
    }

    @Test
    void uploadProductImage_whenFileIsTooLarge_throwsIllegalArgumentException() {
        byte[] large = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", large);
        assertThrows(IllegalArgumentException.class, () -> service.uploadProductImage(file));
    }

    @Test
    void uploadProductImage_whenCloudinaryConfigMissing_throwsIllegalStateException() {
        CloudinaryProperties missing = new CloudinaryProperties();
        missing.setCloudName("");
        missing.setApiKey("");
        missing.setApiSecret("");
        ProductImageUploadService misconfigured = new ProductImageUploadService(cloudinary, missing);

        MockMultipartFile file = new MockMultipartFile("file", "shoe.png", "image/png", new byte[] {1, 2, 3});
        assertThrows(IllegalStateException.class, () -> misconfigured.uploadProductImage(file));
    }
}
