package com.example.ecommerce.product.mapper;

import com.example.ecommerce.product.dto.ProductDto;
import com.example.ecommerce.product.model.Product;

public class ProductMapper {

    public static ProductDto toDto(Product product) {
        if (product == null) return null;

        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setCategory(product.getCategory());
        dto.setBrand(product.getBrand());
        dto.setSku(product.getSku());
        dto.setColor(product.getColor());
        dto.setSize(product.getSize());
        dto.setAttributesJson(product.getAttributesJson());
        dto.setStock(product.getStock());
        return dto;
    }

    public static Product toEntity(ProductDto dto) {
        if (dto == null) return null;

        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(dto.getCategory());
        product.setBrand(dto.getBrand());
        product.setSku(dto.getSku());
        product.setColor(dto.getColor());
        product.setSize(dto.getSize());
        product.setAttributesJson(dto.getAttributesJson());
        product.setStock(dto.getStock());
        return product;
    }
}
