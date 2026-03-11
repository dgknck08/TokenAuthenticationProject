package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.model.ShippingMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CheckoutQuoteResponse {
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String appliedCouponCode;
    private ShippingMethod shippingMethod;
}
