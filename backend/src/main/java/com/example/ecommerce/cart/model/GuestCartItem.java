package com.example.ecommerce.cart.model;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class GuestCartItem implements Serializable {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
}