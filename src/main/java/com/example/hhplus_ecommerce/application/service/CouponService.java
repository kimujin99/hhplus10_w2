package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final CouponService self; // Self-injection for transaction proxy

    // Constructor for self-injection with @Lazy to avoid circular dependency
    public CouponService(CouponRepository couponRepository,
                         UserCouponRepository userCouponRepository,
                         UserRepository userRepository,
                         @Lazy CouponService self) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
        this.self = self;
    }

    public List<CouponResponse> getCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        return CouponResponse.fromList(coupons);
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);
        return CouponResponse.from(coupon);
    }

    private static final int MAX_RETRY_COUNT = 10;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserCouponResponse issueCouponWithRetry(Long userId, IssueCouponRequest request) {
        int retryCount = 0;

        while (true) {
            try {
                // self를 통해 호출하여 트랜잭션 프록시를 거치도록 함
                return self.issueCoupon(userId, request);
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                if (++retryCount >= MAX_RETRY_COUNT) {
                    throw new ConflictException(CouponErrorCode.COUPON_ISSUE_CONFLICT);
                }

                // Exponential backoff with jitter to prevent thundering herd
                try {
                    long backoffMillis = (long) (Math.pow(2, retryCount - 1) * 50 + Math.random() * 50);
                    Thread.sleep(Math.min(backoffMillis, 1000)); // Cap at 1 second
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Transactional
    public UserCouponResponse issueCoupon(Long userId, IssueCouponRequest request) {
        userRepository.findByIdOrThrow(userId);
        Coupon coupon = couponRepository.findByIdOrThrow(request.couponId());

        userCouponRepository.findByUserIdAndCoupon_Id(userId, request.couponId())
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
                .map(userCoupon -> {
                    return UserCouponResponse.from(userCoupon, userCoupon.getCoupon());
                })
                .toList();
    }
}