package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.dto.CouponDto;
import com.example.ecommerce.coupon.model.Coupon;
import com.example.ecommerce.coupon.model.DiscountType;
import com.example.ecommerce.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository);
    }

    private CouponDto validRequest(String code) {
        CouponDto dto = new CouponDto();
        dto.setCode(code);
        dto.setDiscountType(DiscountType.PERCENTAGE);
        dto.setDiscountValue(new BigDecimal("10.00"));
        dto.setMinOrderAmount(new BigDecimal("50.00"));
        dto.setMaxDiscountAmount(new BigDecimal("25.00"));
        dto.setActive(true);
        dto.setMaxRedemptions(100);
        dto.setPerUserLimit(1);
        dto.setStartsAt(Instant.parse("2026-01-01T00:00:00Z"));
        dto.setExpiresAt(Instant.parse("2026-12-31T00:00:00Z"));
        return dto;
    }

    private Coupon existingCoupon(Long id, String code) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCode(code);
        coupon.setDiscountType(DiscountType.FIXED);
        coupon.setDiscountValue(new BigDecimal("5.00"));
        coupon.setActive(true);
        return coupon;
    }

    @Test
    void getAll_shouldMapAllCouponsToDto() {
        Coupon coupon = existingCoupon(7L, "SAVE5");
        coupon.setMinOrderAmount(new BigDecimal("20.00"));
        coupon.setMaxDiscountAmount(new BigDecimal("5.00"));
        coupon.setMaxRedemptions(10);
        coupon.setPerUserLimit(2);
        when(couponRepository.findAll()).thenReturn(List.of(coupon));

        List<CouponDto> result = couponService.getAll();

        assertEquals(1, result.size());
        CouponDto dto = result.get(0);
        assertEquals(7L, dto.getId());
        assertEquals("SAVE5", dto.getCode());
        assertEquals(DiscountType.FIXED, dto.getDiscountType());
        assertEquals(new BigDecimal("5.00"), dto.getDiscountValue());
        assertEquals(10, dto.getMaxRedemptions());
        assertEquals(2, dto.getPerUserLimit());
        assertTrue(dto.isActive());
    }

    @Test
    void create_shouldNormalizeCodeAndPersist() {
        when(couponRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon c = invocation.getArgument(0);
            c.setId(42L);
            return c;
        });

        CouponDto result = couponService.create(validRequest("  summer10 "));

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());
        Coupon saved = captor.getValue();
        assertEquals("SUMMER10", saved.getCode());
        assertEquals(DiscountType.PERCENTAGE, saved.getDiscountType());
        assertEquals(new BigDecimal("10.00"), saved.getDiscountValue());
        assertEquals(42L, result.getId());
        assertEquals("SUMMER10", result.getCode());
    }

    @Test
    void create_shouldRejectDuplicateCode() {
        when(couponRepository.findByCodeIgnoreCase("SUMMER10"))
                .thenReturn(Optional.of(existingCoupon(1L, "SUMMER10")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> couponService.create(validRequest("summer10")));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(couponRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectBlankCode() {
        assertThrows(IllegalArgumentException.class,
                () -> couponService.create(validRequest("   ")));
        verify(couponRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectNullCode() {
        assertThrows(IllegalArgumentException.class,
                () -> couponService.create(validRequest(null)));
    }

    @Test
    void update_shouldModifyExistingCoupon() {
        Coupon existing = existingCoupon(5L, "OLD");
        when(couponRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(couponRepository.findByCodeIgnoreCase("NEW10")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponDto result = couponService.update(5L, validRequest("new10"));

        assertEquals("NEW10", existing.getCode());
        assertEquals(DiscountType.PERCENTAGE, existing.getDiscountType());
        assertEquals("NEW10", result.getCode());
    }

    @Test
    void update_shouldAllowKeepingSameCodeOnSameCoupon() {
        Coupon existing = existingCoupon(5L, "KEEP");
        when(couponRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(couponRepository.findByCodeIgnoreCase("KEEP")).thenReturn(Optional.of(existing));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponDto result = couponService.update(5L, validRequest("keep"));

        assertEquals("KEEP", result.getCode());
    }

    @Test
    void update_shouldRejectWhenCodeUsedByAnotherCoupon() {
        Coupon existing = existingCoupon(5L, "OLD");
        Coupon other = existingCoupon(9L, "TAKEN");
        when(couponRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(couponRepository.findByCodeIgnoreCase("TAKEN")).thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class,
                () -> couponService.update(5L, validRequest("taken")));
        verify(couponRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowWhenCouponNotFound() {
        when(couponRepository.findById(404L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> couponService.update(404L, validRequest("any")));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void delete_shouldRemoveExistingCoupon() {
        Coupon existing = existingCoupon(3L, "DEL");
        when(couponRepository.findById(3L)).thenReturn(Optional.of(existing));

        couponService.delete(3L);

        verify(couponRepository).delete(existing);
    }

    @Test
    void delete_shouldThrowWhenCouponNotFound() {
        when(couponRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> couponService.delete(404L));
        verify(couponRepository, never()).delete(any());
    }
}
