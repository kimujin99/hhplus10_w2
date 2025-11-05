package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;

import java.util.List;

public interface UserCouponRepository {
    UserCoupon findById(Long id);
    UserCoupon save(UserCoupon userCoupon);
    List<UserCoupon> findByUserId(Long userId);
    UserCoupon findByUserIdAndCouponId(Long userId, Long couponId);
}