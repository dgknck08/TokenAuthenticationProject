package com.example.ecommerce.order.service;

import com.example.ecommerce.cart.exception.InsufficientStockException;
import com.example.ecommerce.coupon.model.Coupon;
import com.example.ecommerce.coupon.model.CouponRedemption;
import com.example.ecommerce.coupon.model.DiscountType;
import com.example.ecommerce.coupon.repository.CouponRedemptionRepository;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.inventory.model.InventoryItem;
import com.example.ecommerce.inventory.repository.InventoryRepository;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.model.ShippingMethod;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class CheckoutPricingService {
    private static final int MONEY_SCALE = 2;

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final InventoryRepository inventoryRepository;
    private final BigDecimal taxRate;
    private final BigDecimal standardShippingFee;
    private final BigDecimal expressShippingFee;
    private final BigDecimal freeShippingThreshold;

    public CheckoutPricingService(ProductRepository productRepository,
                                  CouponRepository couponRepository,
                                  CouponRedemptionRepository couponRedemptionRepository,
                                  InventoryRepository inventoryRepository,
                                  @Value("${app.checkout.tax-rate:0.20}") BigDecimal taxRate,
                                  @Value("${app.checkout.standard-shipping-fee:49.90}") BigDecimal standardShippingFee,
                                  @Value("${app.checkout.express-shipping-fee:89.90}") BigDecimal expressShippingFee,
                                  @Value("${app.checkout.free-shipping-threshold:2000.00}") BigDecimal freeShippingThreshold) {
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.inventoryRepository = inventoryRepository;
        this.taxRate = normalizeMoney(taxRate);
        this.standardShippingFee = normalizeMoney(standardShippingFee);
        this.expressShippingFee = normalizeMoney(expressShippingFee);
        this.freeShippingThreshold = normalizeMoney(freeShippingThreshold);
    }

    public OrderPricingResult buildPricing(List<OrderItemRequest> requestedItems,
                                           Long userId,
                                           String couponCode,
                                           ShippingMethod requestedShippingMethod) {
        List<OrderPricingItem> items = resolveItems(requestedItems);
        BigDecimal subtotal = calculateSubtotal(items);

        Coupon appliedCoupon = resolveCoupon(couponCode, userId, subtotal);
        BigDecimal discount = calculateDiscount(subtotal, appliedCoupon);

        ShippingMethod shippingMethod = requestedShippingMethod != null ? requestedShippingMethod : ShippingMethod.STANDARD;
        BigDecimal shippingFee = calculateShippingFee(subtotal.subtract(discount), shippingMethod);
        BigDecimal taxableBase = subtotal.subtract(discount).add(shippingFee);
        BigDecimal taxAmount = taxableBase.multiply(taxRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableBase.add(taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new OrderPricingResult(
                items,
                subtotal,
                discount,
                shippingFee,
                taxAmount,
                totalAmount,
                shippingMethod,
                appliedCoupon != null ? appliedCoupon.getCode() : null,
                appliedCoupon
        );
    }

    public void recordCouponRedemption(OrderPricingResult pricing, Long userId, Long orderId) {
        if (pricing == null || pricing.appliedCoupon() == null || userId == null || orderId == null) {
            return;
        }
        CouponRedemption redemption = new CouponRedemption();
        redemption.setCoupon(pricing.appliedCoupon());
        redemption.setUserId(userId);
        redemption.setOrderId(orderId);
        couponRedemptionRepository.save(redemption);
    }

    public void recordCouponRedemptionByCode(String couponCode, Long userId, Long orderId) {
        String normalizedCode = normalizeCouponCode(couponCode);
        if (normalizedCode == null || userId == null || orderId == null) {
            return;
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode).orElse(null);
        if (coupon == null) {
            return;
        }
        CouponRedemption redemption = new CouponRedemption();
        redemption.setCoupon(coupon);
        redemption.setUserId(userId);
        redemption.setOrderId(orderId);
        couponRedemptionRepository.save(redemption);
    }

    private List<OrderPricingItem> resolveItems(List<OrderItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        Map<Long, Integer> requestedQuantityByProductId = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : requestedItems) {
            if (itemRequest.getProductId() == null) {
                throw new IllegalArgumentException("Product id is required for each order item.");
            }
            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 for product: " + itemRequest.getProductId());
            }
            requestedQuantityByProductId.merge(itemRequest.getProductId(), itemRequest.getQuantity(), Integer::sum);
        }

        List<Long> productIds = new ArrayList<>(requestedQuantityByProductId.keySet());
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            productsById.put(product.getId(), product);
        }
        if (productsById.size() != productIds.size()) {
            List<Long> missingProductIds = productIds.stream()
                    .filter(productId -> !productsById.containsKey(productId))
                    .toList();
            throw new IllegalArgumentException("Product not found: " + missingProductIds);
        }

        Map<Long, Integer> availableStockByProductId = new HashMap<>();
        for (InventoryItem inventoryItem : inventoryRepository.findByProductIdIn(productIds)) {
            availableStockByProductId.put(inventoryItem.getProduct().getId(), inventoryItem.getAvailableStock());
        }

        for (Long productId : productIds) {
            Product product = productsById.get(productId);
            int available = availableStockByProductId.getOrDefault(productId, Math.max(product.getStock(), 0));
            int requestedQuantity = requestedQuantityByProductId.get(productId);
            if (requestedQuantity > available) {
                throw new InsufficientStockException(
                        "Insufficient stock for product " + productId + ". Available: " + available + ", Requested: " + requestedQuantity
                );
            }
        }

        List<OrderPricingItem> resolved = new ArrayList<>();
        for (OrderItemRequest itemRequest : requestedItems) {
            Product product = productsById.get(itemRequest.getProductId());
            resolved.add(new OrderPricingItem(product, itemRequest.getQuantity()));
        }
        return resolved;
    }

    private BigDecimal calculateSubtotal(List<OrderPricingItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderPricingItem item : items) {
            subtotal = subtotal.add(item.product().getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        return subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private Coupon resolveCoupon(String rawCouponCode, Long userId, BigDecimal subtotal) {
        String couponCode = normalizeCouponCode(rawCouponCode);
        if (couponCode == null) {
            return null;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found or inactive."));

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon is inactive.");
        }
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Coupon is not active yet.");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Coupon has expired.");
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Coupon minimum order amount is not satisfied.");
        }
        if (coupon.getMaxRedemptions() != null) {
            long totalRedemptions = couponRedemptionRepository.countByCoupon_Id(coupon.getId());
            if (totalRedemptions >= coupon.getMaxRedemptions()) {
                throw new IllegalArgumentException("Coupon redemption limit reached.");
            }
        }
        if (coupon.getPerUserLimit() != null && userId != null) {
            long userRedemptions = couponRedemptionRepository.countByCoupon_IdAndUserId(coupon.getId(), userId);
            if (userRedemptions >= coupon.getPerUserLimit()) {
                throw new IllegalArgumentException("Coupon per-user redemption limit reached.");
            }
        }

        return coupon;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, Coupon coupon) {
        if (coupon == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal rawDiscount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            rawDiscount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
        } else {
            rawDiscount = coupon.getDiscountValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        if (coupon.getMaxDiscountAmount() != null && rawDiscount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            rawDiscount = coupon.getMaxDiscountAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        if (rawDiscount.compareTo(subtotal) > 0) {
            rawDiscount = subtotal;
        }

        return rawDiscount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateShippingFee(BigDecimal discountedSubtotal, ShippingMethod shippingMethod) {
        if (shippingMethod == ShippingMethod.EXPRESS) {
            return expressShippingFee;
        }
        if (discountedSubtotal.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return standardShippingFee;
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
