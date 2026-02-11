package com.example.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private List<CartItemDto> items;
    private int totalItems;
    private BigDecimal totalAmount;
    private String cartType; // "authenticated" or "guest"
}
