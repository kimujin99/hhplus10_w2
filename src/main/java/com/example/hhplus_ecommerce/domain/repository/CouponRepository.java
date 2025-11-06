package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> findById(Long couponId);
    Coupon save(Coupon coupon);
    List<Coupon> findAll();
}
