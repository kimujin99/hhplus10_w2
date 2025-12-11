package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.CouponResponse;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.IssueCouponRequest;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.UserCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@EnableRetry
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public List<CouponResponse> getCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        return CouponResponse.fromList(coupons);
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);
        return CouponResponse.from(coupon);
    }

    @Transactional
    public UserCouponResponse issueCoupon(Long userId, IssueCouponRequest request) {
        userRepository.findByIdOrThrow(userId);
        Coupon coupon = couponRepository.findByIdWithLockOrThrow(request.couponId());

        userCouponRepository.findByUserIdAndCouponIdWithLock(userId, request.couponId())
            .ifPresent(uc -> {
                throw new ConflictException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            });

        // 쿠폰 발급 (재고 검증 + 차감)
        coupon.issue();
        couponRepository.save(coupon);

        // UserCoupon 저장
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        return UserCouponResponse.from(savedUserCoupon, coupon);
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        userRepository.findByIdOrThrow(userId);
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        return userCoupons.stream()
                .map(userCoupon -> UserCouponResponse.from(userCoupon, userCoupon.getCoupon()))
                .toList();
    }
}