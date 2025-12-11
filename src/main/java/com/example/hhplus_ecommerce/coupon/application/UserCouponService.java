package com.example.hhplus_ecommerce.coupon.application;

import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import com.example.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 쿠폰 관리 서비스
 * <p>
 * 사용자 쿠폰의 사용 및 복구를 담당합니다.
 * 낙관적 락(Optimistic Lock)을 사용하여 쿠폰 동시성을 제어합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;

    /**
     * 사용자 쿠폰을 사용 처리합니다.
     * <p>
     * 동시성 제어:
     * - 낙관적 락(@Version)을 사용하여 쿠폰별 동시성 제어
     * - 동시 요청 시 ObjectOptimisticLockingFailureException 발생 (재시도 없음)
     * - 새로운 트랜잭션에서 실행되어 독립적으로 커밋/롤백 (Saga 패턴)
     *
     * @param userCouponId 사용할 사용자 쿠폰 ID
     * @throws NotFoundException 쿠폰을 찾을 수 없는 경우
     * @throws ConflictException 쿠폰이 이미 사용된 경우
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 낙관적 락 충돌 시 발생
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void useCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(userCouponId);
        userCoupon.use();
        userCouponRepository.save(userCoupon);

        log.info("쿠폰 사용 성공: userCouponId={}", userCouponId);
    }

    /**
     * 사용자 쿠폰을 미사용 상태로 복구합니다.
     * <p>
     * Saga 패턴의 보상 트랜잭션으로 사용됩니다.
     * 결제 실패 시 사용된 쿠폰을 미사용 상태로 원복합니다.
     * <p>
     * 동시성 제어:
     * - 낙관적 락(@Version)을 사용하여 쿠폰별 동시성 제어
     * - 동시 요청 시 ObjectOptimisticLockingFailureException 발생 (재시도 없음)
     * - 새로운 트랜잭션에서 실행되어 독립적으로 커밋/롤백 (Saga 패턴)
     *
     * @param userCouponId 복구할 사용자 쿠폰 ID
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 낙관적 락 충돌 시 발생
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelUseCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(userCouponId);
        userCoupon.cancelUse();
        userCouponRepository.save(userCoupon);

        log.info("쿠폰 복구 성공: userCouponId={}", userCouponId);
    }
}