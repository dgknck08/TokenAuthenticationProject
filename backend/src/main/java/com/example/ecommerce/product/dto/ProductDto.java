package com.example.ecommerce.product.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private String brand;
    private String sku;
    private String color;
    private String size;
    private String attributesJson;
    private int stock;

    public ProductDto(Long id, String name, String description, BigDecimal price, String imageUrl, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }
}
