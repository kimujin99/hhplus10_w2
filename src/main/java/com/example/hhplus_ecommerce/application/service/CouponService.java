package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        Coupon coupon = couponRepository.findByIdOrThrow(request.couponId());

        userCouponRepository.findByUserIdAndCouponId(userId, request.couponId())
            .ifPresent(uc -> {
                throw new ConflictException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            });

        // 쿠폰 발급 (재고 검증 + 차감)
        coupon.issue();
        couponRepository.save(coupon);

        // UserCoupon 저장
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(request.couponId())
                .build();
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        return UserCouponResponse.from(savedUserCoupon, coupon);
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        userRepository.findByIdOrThrow(userId);
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .toList();

        // TODO: 실제 DB로 전환시 로직 제거. JOIN으로 처리
        Map<Long, Coupon> couponMap = couponIds.stream()
                .map(couponId -> couponRepository.findById(couponId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Coupon::getId, coupon -> coupon));

        return userCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = couponMap.get(userCoupon.getCouponId());
                    return UserCouponResponse.from(userCoupon, coupon);
                })
                .toList();
    }
}