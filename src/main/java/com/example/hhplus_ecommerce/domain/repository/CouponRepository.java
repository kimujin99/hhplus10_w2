package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.Coupon;

import java.util.List;

public interface CouponRepository {
    Coupon findById(Long couponId);
    Coupon save(Coupon coupon);
    List<Coupon> findAll();
}
