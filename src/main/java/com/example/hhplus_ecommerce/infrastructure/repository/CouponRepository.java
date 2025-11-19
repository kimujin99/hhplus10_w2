package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findById(Long couponId);

    default Coupon findByIdOrThrow(Long couponId) {
        return findById(couponId)
                .orElseThrow(() -> new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(@Param("id") Long id);

    default Coupon findByIdWithLockOrThrow(Long couponId) {
        return findByIdWithLock(couponId)
                .orElseThrow(() -> new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    Coupon save(Coupon coupon);
    List<Coupon> findAll();
}
