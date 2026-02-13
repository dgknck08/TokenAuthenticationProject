package com.example.ecommerce.product.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private String brand;
    @Column(unique = true)
    private String sku;
    private String color;
    private String size;
    @Column(columnDefinition = "TEXT")
    private String attributesJson;
    private int stock;

    public Product(Long id, String name, String description, BigDecimal price, String imageUrl, String category, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.stock = stock;
    }
}
