package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<UserCoupon> findByCoupon_Id(Long couponId);
    Optional<UserCoupon> findByUserIdAndCoupon_Id(Long userId, Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.coupon.id = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponIdWithLock(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithLock(@Param("id") Long id);

    default UserCoupon findByIdWithLockOrThrow(Long userCouponId) {
        return findByIdWithLock(userCouponId)
                .orElseThrow(() -> new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND));
    }

}