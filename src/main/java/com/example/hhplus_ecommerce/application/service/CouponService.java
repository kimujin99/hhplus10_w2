package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.lock.DistributedLock;
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
import java.util.concurrent.TimeUnit;

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

    /**
     * 쿠폰을 발급합니다.
     * <p>
     * 사용자에게 쿠폰을 발급하며, 쿠폰 재고 확인, 중복 발급 검증 등을 수행합니다.
     * 선착순 쿠폰의 경우 재고가 소진되면 발급이 불가능합니다.
     * <p>
     * 동시성 제어:
     * - 분산 락(Redisson RLock)을 사용하여 사용자별 + 쿠폰별 동시성 제어
     * - 다중 서버 환경에서 같은 사용자의 중복 쿠폰 발급 방지
     * - DB 비관적 락과 함께 사용하여 쿠폰 재고 정합성 보장
     *
     * @param userId 쿠폰을 발급받을 사용자 ID
     * @param request 쿠폰 발급 요청 정보 (쿠폰 ID)
     * @return 발급된 사용자 쿠폰 정보
     * @throws NotFoundException 사용자 또는 쿠폰을 찾을 수 없는 경우
     * @throws ConflictException 쿠폰이 이미 발급되었거나 재고가 없는 경우
     */
    @DistributedLock(
        key = "coupon:issue:user:#{#userId}:coupon:#{#request.couponId}",
        waitTime = 5L,
        leaseTime = 3L,
        timeUnit = TimeUnit.SECONDS
    )
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