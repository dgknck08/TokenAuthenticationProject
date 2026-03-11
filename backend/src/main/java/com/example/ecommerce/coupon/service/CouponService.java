package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.dto.CouponDto;
import com.example.ecommerce.coupon.model.Coupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getAll() {
        return couponRepository.findAll().stream().map(this::toDto).toList();
    }

    public CouponDto create(CouponDto request) {
        String code = normalizeCode(request.getCode());
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists.");
        }
        Coupon coupon = mapToEntity(new Coupon(), request);
        coupon.setCode(code);
        return toDto(couponRepository.save(coupon));
    }

    public CouponDto update(Long id, CouponDto request) {
        Coupon existing = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
        String code = normalizeCode(request.getCode());
        couponRepository.findByCodeIgnoreCase(code)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("Coupon code already exists.");
                });
        existing.setCode(code);
        mapToEntity(existing, request);
        return toDto(couponRepository.save(existing));
    }

    public void delete(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
        couponRepository.delete(coupon);
    }

    private Coupon mapToEntity(Coupon target, CouponDto source) {
        target.setDiscountType(source.getDiscountType());
        target.setDiscountValue(source.getDiscountValue());
        target.setMinOrderAmount(source.getMinOrderAmount());
        target.setMaxDiscountAmount(source.getMaxDiscountAmount());
        target.setActive(source.isActive());
        target.setMaxRedemptions(source.getMaxRedemptions());
        target.setPerUserLimit(source.getPerUserLimit());
        target.setStartsAt(source.getStartsAt());
        target.setExpiresAt(source.getExpiresAt());
        return target;
    }

    private CouponDto toDto(Coupon coupon) {
        CouponDto dto = new CouponDto();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMinOrderAmount(coupon.getMinOrderAmount());
        dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        dto.setActive(coupon.isActive());
        dto.setMaxRedemptions(coupon.getMaxRedemptions());
        dto.setPerUserLimit(coupon.getPerUserLimit());
        dto.setStartsAt(coupon.getStartsAt());
        dto.setExpiresAt(coupon.getExpiresAt());
        return dto;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code is required.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
