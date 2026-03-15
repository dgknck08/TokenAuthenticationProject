package com.example.ecommerce.product.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    @NotBlank(message = "Product name is required.")
    @Size(max = 255, message = "Product name is too long.")
    private String name;
    @NotBlank(message = "Product description is required.")
    @Size(max = 2000, message = "Product description is too long.")
    private String description;
    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0.")
    private BigDecimal price;
    @Size(max = 2048, message = "Image URL is too long.")
    private String imageUrl;
    @NotBlank(message = "Category is required.")
    @Size(max = 120, message = "Category is too long.")
    private String category;
    @Size(max = 120, message = "Brand is too long.")
    private String brand;
    @Size(max = 120, message = "SKU is too long.")
    private String sku;
    @Size(max = 120, message = "Color is too long.")
    private String color;
    @Size(max = 120, message = "Size is too long.")
    private String size;
    @Size(max = 5000, message = "Attributes payload is too long.")
    private String attributesJson;
    @Min(value = 0, message = "Stock must be greater than or equal to 0.")
    private int stock;

    public ProductDto(Long id, String name, String description, BigDecimal price, String imageUrl, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    @AssertTrue(message = "Image URL must be an http(s) URL or /products path ending with a supported image extension.")
    public boolean isImageUrlValid() {
        if (imageUrl == null || imageUrl.isBlank()) {
            return true;
        }

        String normalized = imageUrl.trim().toLowerCase();
        boolean validPrefix = normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("/products/");
        if (!validPrefix) {
            return false;
        }

        int queryIndex = normalized.indexOf('?');
        String path = queryIndex >= 0 ? normalized.substring(0, queryIndex) : normalized;
        return path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".webp")
                || path.endsWith(".gif");
    }
}
