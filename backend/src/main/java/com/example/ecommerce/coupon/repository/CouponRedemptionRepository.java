package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCoupon_Id(Long couponId);

    long countByCoupon_IdAndUserId(Long couponId, Long userId);
}
