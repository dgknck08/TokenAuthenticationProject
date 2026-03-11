package com.example.ecommerce.order.service;

import com.example.ecommerce.product.model.Product;

public record OrderPricingItem(
        Product product,
        int quantity
) {}
