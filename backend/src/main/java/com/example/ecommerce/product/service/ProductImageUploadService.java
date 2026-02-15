package com.example.ecommerce.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.ecommerce.config.CloudinaryProperties;
import com.example.ecommerce.product.dto.ProductImageUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageUploadService {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public ProductImageUploadService(Cloudinary cloudinary, CloudinaryProperties cloudinaryProperties) {
        this.cloudinary = cloudinary;
        this.cloudinaryProperties = cloudinaryProperties;
    }

    public ProductImageUploadResponse uploadProductImage(MultipartFile file) {
        validateCloudinaryConfig();
        validateFile(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", cloudinaryProperties.getFolder(),
                            "resource_type", "image",
                            "public_id", buildPublicId(file.getOriginalFilename()),
                            "overwrite", false
                    )
            );

            return new ProductImageUploadResponse(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("format"),
                    (Integer) result.get("width"),
                    (Integer) result.get("height"),
                    result.get("bytes") == null ? null : ((Number) result.get("bytes")).longValue()
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Image upload failed.", ex);
        }
    }

    private void validateCloudinaryConfig() {
        if (isBlank(cloudinaryProperties.getCloudName()) || isBlank(cloudinaryProperties.getApiKey()) || isBlank(cloudinaryProperties.getApiSecret())) {
            throw new IllegalStateException("Cloudinary credentials are missing.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file size must be 5MB or smaller.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only PNG, JPG, JPEG, and WEBP images are allowed.");
        }
    }

    private String buildPublicId(String originalFilename) {
        String base = originalFilename == null ? "product-image" : originalFilename;
        int dotIndex = base.lastIndexOf('.');
        if (dotIndex > 0) {
            base = base.substring(0, dotIndex);
        }
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
