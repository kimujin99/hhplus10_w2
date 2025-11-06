package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    Optional<UserCoupon> findById(Long userCouponId);
    UserCoupon save(UserCoupon userCoupon);
    List<UserCoupon> findByUserId(Long userId);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    Optional<UserCoupon> findByOrderId(Long orderId);
}