package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.lock.DistributedLock;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 사용자 쿠폰 관리 서비스
 * <p>
 * 사용자 쿠폰의 사용 및 복구를 담당합니다.
 * 분산 락을 사용하여 다중 서버 환경에서의 쿠폰 동시성을 제어합니다.
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
     * - 분산 락(Redisson RLock)을 사용하여 쿠폰별 동시성 제어
     * - 다중 서버 환경에서 같은 쿠폰의 중복 사용 방지
     * - 새로운 트랜잭션에서 실행되어 독립적으로 커밋/롤백
     *
     * @param userCouponId 사용할 사용자 쿠폰 ID
     * @throws NotFoundException 쿠폰을 찾을 수 없는 경우
     * @throws ConflictException 쿠폰이 이미 사용된 경우
     */
    @DistributedLock(
        key = "userCoupon:#{#userCouponId}",
        waitTime = 10L,
        leaseTime = 5L,
        timeUnit = TimeUnit.SECONDS
    )
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
     *
     * @param userCouponId 복구할 사용자 쿠폰 ID
     */
    @DistributedLock(
        key = "userCoupon:#{#userCouponId}",
        waitTime = 10L,
        leaseTime = 5L,
        timeUnit = TimeUnit.SECONDS
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelUseCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(userCouponId);
        userCoupon.cancelUse();
        userCouponRepository.save(userCoupon);

        log.info("쿠폰 복구 성공: userCouponId={}", userCouponId);
    }
}