package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    Optional<UserCoupon> findById(Long userCouponId);

    default UserCoupon findByIdOrThrow(Long userCouponId) {
        return findById(userCouponId)
                .orElseThrow(() -> new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    UserCoupon save(UserCoupon userCoupon);
    List<UserCoupon> findByUserId(Long userId);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    Optional<UserCoupon> findByOrderId(Long orderId);
}