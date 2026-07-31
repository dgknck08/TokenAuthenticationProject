package com.example.ecommerce.service;

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
import com.example.ecommerce.order.service.CheckoutPricingService;
import com.example.ecommerce.order.service.OrderPricingResult;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckoutPricingServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    private CheckoutPricingService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutPricingService(
                productRepository,
                couponRepository,
                couponRedemptionRepository,
                inventoryRepository,
                new BigDecimal("0.20"),
                new BigDecimal("49.90"),
                new BigDecimal("89.90"),
                new BigDecimal("2000.00"));
    }

    private Product product(Long id, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    private OrderItemRequest orderItem(Long productId, int quantity) {
        OrderItemRequest r = new OrderItemRequest();
        r.setProductId(productId);
        r.setQuantity(quantity);
        return r;
    }

    private InventoryItem inventory(Product product, int available) {
        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setAvailableStock(available);
        return item;
    }

    private Coupon coupon(DiscountType type, String value) {
        Coupon c = new Coupon();
        c.setId(100L);
        c.setCode("SAVE");
        c.setDiscountType(type);
        c.setDiscountValue(new BigDecimal(value));
        c.setActive(true);
        return c;
    }

    private void stockAvailable(Product product, int available) {
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(inventoryRepository.findByProductIdIn(anyList()))
                .thenReturn(List.of(inventory(product, available)));
    }

    // ── buildPricing happy paths ──────────────────────────────
    @Test
    void buildPricing_noCoupon_standardShipping() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 2)), 7L, null, ShippingMethod.STANDARD);

        assertEquals(new BigDecimal("200.00"), result.subtotalAmount());
        assertEquals(new BigDecimal("0.00"), result.discountAmount());
        assertEquals(new BigDecimal("49.90"), result.shippingFee());
        assertEquals(new BigDecimal("49.98"), result.taxAmount());
        assertEquals(new BigDecimal("299.88"), result.totalAmount());
        assertEquals(ShippingMethod.STANDARD, result.shippingMethod());
        assertNull(result.appliedCouponCode());
    }

    @Test
    void buildPricing_freeShippingWhenAboveThreshold() {
        Product p = product(1L, "2000.00", 10);
        stockAvailable(p, 10);

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 1)), null, null, null);

        assertEquals(new BigDecimal("0.00"), result.shippingFee());
        assertEquals(ShippingMethod.STANDARD, result.shippingMethod());
    }

    @Test
    void buildPricing_expressShippingAlwaysCharged() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 1)), null, null, ShippingMethod.EXPRESS);

        assertEquals(new BigDecimal("89.90"), result.shippingFee());
    }

    @Test
    void buildPricing_percentageCouponApplied() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        when(couponRepository.findByCodeIgnoreCase("SAVE"))
                .thenReturn(Optional.of(coupon(DiscountType.PERCENTAGE, "10")));

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 2)), 7L, "save", ShippingMethod.STANDARD);

        assertEquals(new BigDecimal("200.00"), result.subtotalAmount());
        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals("SAVE", result.appliedCouponCode());
    }

    // ── resolveItems validation ───────────────────────────────
    @Test
    void buildPricing_shouldRejectEmptyItems() {
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(), 1L, null, null));
    }

    @Test
    void buildPricing_shouldRejectNullProductId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(null, 1)), 1L, null, null));
    }

    @Test
    void buildPricing_shouldRejectNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 0)), 1L, null, null));
    }

    @Test
    void buildPricing_shouldRejectMissingProduct() {
        when(productRepository.findAllById(anyList())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, null, null));
    }

    @Test
    void buildPricing_shouldRejectInsufficientStock() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 1);
        assertThrows(InsufficientStockException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 5)), 1L, null, null));
    }

    @Test
    void buildPricing_shouldFallBackToProductStockWhenNoInventoryRow() {
        Product p = product(1L, "100.00", 5);
        when(productRepository.findAllById(anyList())).thenReturn(List.of(p));
        when(inventoryRepository.findByProductIdIn(anyList())).thenReturn(List.of());

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 3)), 1L, null, ShippingMethod.STANDARD);

        assertEquals(new BigDecimal("300.00"), result.subtotalAmount());
    }

    @Test
    void buildPricing_shouldMergeDuplicateProductQuantities() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 2), orderItem(1L, 1)), 1L, null, ShippingMethod.STANDARD);

        assertEquals(2, result.items().size());
    }

    // ── resolveCoupon validation ──────────────────────────────
    @Test
    void buildPricing_couponNotFound_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, "save", null));
    }

    @Test
    void buildPricing_inactiveCoupon_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "10");
        c.setActive(false);
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, "save", null));
    }

    @Test
    void buildPricing_couponNotStarted_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "10");
        c.setStartsAt(Instant.now().plusSeconds(3600));
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, "save", null));
    }

    @Test
    void buildPricing_couponExpired_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "10");
        c.setExpiresAt(Instant.now().minusSeconds(3600));
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, "save", null));
    }

    @Test
    void buildPricing_couponMinOrderNotMet_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "10");
        c.setMinOrderAmount(new BigDecimal("500.00"));
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, "save", null));
    }

    @Test
    void buildPricing_couponMaxRedemptionsReached_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "10");
        c.setMaxRedemptions(2);
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));
        when(couponRedemptionRepository.countByCoupon_Id(100L)).thenReturn(2L);
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 1L, "save", null));
    }

    @Test
    void buildPricing_couponPerUserLimitReached_shouldThrow() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "10");
        c.setPerUserLimit(1);
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));
        when(couponRedemptionRepository.countByCoupon_IdAndUserId(100L, 7L)).thenReturn(1L);
        assertThrows(IllegalArgumentException.class,
                () -> service.buildPricing(List.of(orderItem(1L, 1)), 7L, "save", null));
    }

    // ── discount edge cases ───────────────────────────────────
    @Test
    void buildPricing_fixedDiscountCappedByMaxDiscount() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "50");
        c.setMaxDiscountAmount(new BigDecimal("30.00"));
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 2)), 1L, "save", ShippingMethod.STANDARD);

        assertEquals(new BigDecimal("30.00"), result.discountAmount());
    }

    @Test
    void buildPricing_discountCannotExceedSubtotal() {
        Product p = product(1L, "100.00", 10);
        stockAvailable(p, 10);
        Coupon c = coupon(DiscountType.FIXED, "500");
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));

        OrderPricingResult result = service.buildPricing(
                List.of(orderItem(1L, 1)), 1L, "save", ShippingMethod.STANDARD);

        assertEquals(new BigDecimal("100.00"), result.discountAmount());
    }

    // ── redemption recording ──────────────────────────────────
    @Test
    void recordCouponRedemption_shouldSaveWhenValid() {
        OrderPricingResult pricing = new OrderPricingResult(
                List.of(), new BigDecimal("100.00"), new BigDecimal("10.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("90.00"),
                ShippingMethod.STANDARD, "SAVE", coupon(DiscountType.FIXED, "10"));

        service.recordCouponRedemption(pricing, 7L, 55L);

        verify(couponRedemptionRepository).save(any(CouponRedemption.class));
    }

    @Test
    void recordCouponRedemption_shouldSkipWhenNoCoupon() {
        OrderPricingResult pricing = new OrderPricingResult(
                List.of(), new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.00"),
                ShippingMethod.STANDARD, null, null);

        service.recordCouponRedemption(pricing, 7L, 55L);

        verify(couponRedemptionRepository, never()).save(any());
    }

    @Test
    void recordCouponRedemption_shouldSkipOnNullArgs() {
        service.recordCouponRedemption(null, 7L, 55L);
        verify(couponRedemptionRepository, never()).save(any());
    }

    @Test
    void recordCouponRedemptionByCode_shouldSaveWhenCouponExists() {
        when(couponRepository.findByCodeIgnoreCase("SAVE"))
                .thenReturn(Optional.of(coupon(DiscountType.FIXED, "10")));

        service.recordCouponRedemptionByCode("save", 7L, 55L);

        verify(couponRedemptionRepository).save(any(CouponRedemption.class));
    }

    @Test
    void recordCouponRedemptionByCode_shouldSkipWhenCodeBlank() {
        service.recordCouponRedemptionByCode("  ", 7L, 55L);
        verify(couponRedemptionRepository, never()).save(any());
    }

    @Test
    void recordCouponRedemptionByCode_shouldSkipWhenCouponMissing() {
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.empty());
        service.recordCouponRedemptionByCode("save", 7L, 55L);
        verify(couponRedemptionRepository, never()).save(any());
    }
}
