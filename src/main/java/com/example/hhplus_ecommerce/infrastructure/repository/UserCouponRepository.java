package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    Optional<UserCoupon> findById(Long userCouponId);
    UserCoupon save(UserCoupon userCoupon);
    List<UserCoupon> findByUserId(Long userId);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    Optional<UserCoupon> findByOrderId(Long orderId);
}