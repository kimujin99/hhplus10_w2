package com.example.hhplus_ecommerce.user.application;

import com.example.hhplus_ecommerce.user.domain.PointHistory;
import com.example.hhplus_ecommerce.user.domain.User;
import com.example.hhplus_ecommerce.common.infrastructure.lock.DistributedLock;
import com.example.hhplus_ecommerce.user.infrastructure.PointHistoryRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.user.presentation.dto.UserDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 포인트 관리 서비스
 * <p>
 * 사용자 포인트의 충전, 사용 및 복구를 담당합니다.
 * 분산 락을 사용하여 다중 서버 환경에서의 포인트 동시성을 제어합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPointService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 사용자 포인트를 차감합니다.
     * <p>
     * 동시성 제어:
     * - 분산 락(Redisson RLock)을 사용하여 사용자별 동시성 제어
     * - 다중 서버 환경에서 같은 사용자의 포인트 차감 순차 처리
     * - 새로운 트랜잭션에서 실행되어 독립적으로 커밋/롤백
     *
     * @param userId 포인트를 차감할 사용자 ID
     * @param amount 차감할 포인트 금액
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     * @throws ConflictException 포인트가 부족한 경우
     */
    @DistributedLock(key = "'user:' + #userId + ':point'")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void usePoint(Long userId, Long orderId, Long amount) {
        User user = userRepository.findByIdOrThrow(userId);
        user.usePoint(amount);
        User savedUser = userRepository.save(user);

        PointHistory pointHistory = PointHistory.builder()
                .userId(userId)
                .orderId(orderId)
                .transactionType(PointHistory.TransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(savedUser.getPoint())
                .build();
        pointHistoryRepository.save(pointHistory);

        log.info("포인트 차감 성공: userId={}, amount={}, remainingPoint={}",
            userId, amount, user.getPoint());
    }

    /**
     * 사용자 포인트를 충전합니다.
     * <p>
     * 동시성 제어:
     * - 분산 락(Redisson RLock)을 사용하여 사용자별 동시성 제어
     * - 다중 서버 환경에서 같은 사용자의 포인트 충전 순차 처리
     * - 충전-사용이 동시에 발생해도 정합성 보장
     * - 일반 트랜잭션으로 실행 (보상 트랜잭션 불필요)
     *
     * @param userId 포인트를 충전할 사용자 ID
     * @param amount 충전할 포인트 금액
     * @return 충전 후 포인트 정보
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     */
    @DistributedLock(key = "'user:' + #userId + ':point'")
    @Transactional
    public PointResponse chargePoint(Long userId, ChargePointRequest request) {
        User user = userRepository.findByIdOrThrow(userId);
        user.chargePoint(request.amount());
        User savedUser = userRepository.save(user);

        PointHistory pointHistory = PointHistory.builder()
                .userId(userId)
                .transactionType(PointHistory.TransactionType.CHARGE)
                .amount(request.amount())
                .balanceAfter(savedUser.getPoint())
                .build();
        pointHistoryRepository.save(pointHistory);

        log.info("포인트 충전 성공: userId={}, amount={}, currentPoint={}",
            userId, request.amount(), savedUser.getPoint());

        return PointResponse.from(savedUser);
    }

    /**
     * 사용자 포인트를 복구합니다.
     * <p>
     * Saga 패턴의 보상 트랜잭션으로 사용됩니다.
     * 결제 실패 시 차감된 포인트를 원복합니다.
     *
     * @param userId 포인트를 복구할 사용자 ID
     * @param amount 복구할 포인트 금액
     */
    @DistributedLock(key = "'user:' + #userId + ':point'")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundPoint(Long userId, Long amount) {
        User user = userRepository.findByIdOrThrow(userId);
        user.chargePoint(amount);
        userRepository.save(user);

        log.info("포인트 복구 성공: userId={}, amount={}, currentPoint={}",
            userId, amount, user.getPoint());
    }
}