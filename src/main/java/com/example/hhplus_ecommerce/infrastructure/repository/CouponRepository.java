package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findById(Long couponId);

    default Coupon findByIdOrThrow(Long couponId) {
        return findById(couponId)
                .orElseThrow(() -> new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND));
    }
    
    Coupon save(Coupon coupon);
    List<Coupon> findAll();
}
