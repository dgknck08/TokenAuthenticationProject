package com.example.ecommerce.coupon.dto;

import com.example.ecommerce.coupon.model.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class CouponDto {
    private Long id;

    @NotBlank(message = "Coupon code is required.")
    @Size(max = 64, message = "Coupon code is too long.")
    private String code;

    @NotNull(message = "Discount type is required.")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required.")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0.")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Min order amount cannot be negative.")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", inclusive = true, message = "Max discount amount cannot be negative.")
    private BigDecimal maxDiscountAmount;

    private boolean active = true;
    private Integer maxRedemptions;
    private Integer perUserLimit;
    private Instant startsAt;
    private Instant expiresAt;
}
