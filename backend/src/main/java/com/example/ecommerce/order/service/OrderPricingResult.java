package com.example.ecommerce.order.service;

import com.example.ecommerce.coupon.model.Coupon;
import com.example.ecommerce.order.model.ShippingMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderPricingResult(
        List<OrderPricingItem> items,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        ShippingMethod shippingMethod,
        String appliedCouponCode,
        Coupon appliedCoupon
) {}
