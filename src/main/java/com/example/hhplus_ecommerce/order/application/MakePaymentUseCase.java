package com.example.hhplus_ecommerce.order.application;

import com.example.hhplus_ecommerce.product.application.PopularProductCacheService;
import com.example.hhplus_ecommerce.product.application.ProductStockService;
import com.example.hhplus_ecommerce.coupon.application.UserCouponService;
import com.example.hhplus_ecommerce.user.application.UserPointService;
import com.example.hhplus_ecommerce.order.domain.Order;
import com.example.hhplus_ecommerce.order.domain.OrderItem;
import com.example.hhplus_ecommerce.order.infrastructure.OrderItemRepository;
import com.example.hhplus_ecommerce.order.infrastructure.OrderRepository;
import com.example.hhplus_ecommerce.order.presentation.dto.OrderDto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 결제 처리 유즈케이스
 * <p>
 * Saga 패턴을 적용하여 분산 트랜잭션을 관리합니다.
 * 각 단계(재고 차감, 쿠폰 사용, 포인트 차감)는 독립적인 트랜잭션으로 실행되며,
 * 실패 시 보상 트랜잭션을 통해 롤백됩니다.
 * <p>
 * 동시성 제어:
 * - 분산 락(Redisson RLock)을 사용하여 주문 단위로 중복 결제 방지
 * - 각 서비스는 리소스별 분산 락으로 동시성 제어
 * - Saga 패턴으로 분산 트랜잭션의 일관성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MakePaymentUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductStockService productStockService;
    private final UserCouponService userCouponService;
    private final UserPointService userPointService;
    private final PopularProductCacheService popularProductCacheService;

    /**
     * 주문에 대한 결제를 처리합니다.
     * <p>
     * Saga 패턴 실행 순서:
     * 1. 재고 차감 (여러 상품)
     * 2. 쿠폰 사용 처리 (있는 경우)
     * 3. 포인트 차감
     * 4. 주문 확정
     * <p>
     * 실패 시 보상 트랜잭션:
     * - 포인트 차감 실패 → 쿠폰 복구 → 재고 복구
     * - 쿠폰 사용 실패 → 재고 복구
     * - 재고 차감 실패 → 즉시 실패
     *
     * @param orderId 결제할 주문 ID
     * @return 결제 완료된 주문 정보
     * @throws NotFoundException 주문을 찾을 수 없는 경우
     * @throws ConflictException 재고/포인트 부족, 쿠폰 사용 불가 등
     */
    @Transactional
    public PaymentResponse execute(Long orderId) {
        Order order = orderRepository.findByIdOrThrow(orderId);
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // Saga 보상 트랜잭션을 위한 실행 이력 추적
        List<Long> processedProductIds = new ArrayList<>();
        boolean couponUsed = false;
        boolean pointUsed = false;

        try {
            List<OrderItem> sortedOrderItems = orderItems.stream()
                    .sorted(Comparator.comparing(OrderItem::getProductId))
                    .toList();

            for (OrderItem item : sortedOrderItems) {
                log.info("재고 차감 시작: productId={}, quantity={}", item.getProductId(), item.getQuantity());
                productStockService.decreaseStock(item.getProductId(), item.getQuantity());
                processedProductIds.add(item.getProductId());
            }

            if (order.getUserCouponId() != null) {
                log.info("쿠폰 사용 시작: userCouponId={}", order.getUserCouponId());
                userCouponService.useCoupon(order.getUserCouponId());
                couponUsed = true;
            }

            log.info("포인트 차감 시작: userId={}, amount={}", order.getUserId(), order.getFinalAmount());
            userPointService.usePoint(order.getUserId(), orderId, order.getFinalAmount());
            pointUsed = true;

            order.confirm();
            orderRepository.save(order);

            log.info("결제 완료: orderId={}", orderId);

            // Redis 캐시에 구매 점수 증가 (비동기)
            updatePopularityScore(orderItems);

            return PaymentResponse.from(order);

        } catch (Exception e) {
            log.error("결제 실패: orderId={}, error={}", orderId, e.getMessage());

            // Saga 보상 트랜잭션 실행 (역순으로 롤백)
            compensate(processedProductIds, orderItems, order.getUserCouponId(), order.getUserId(),
                    order.getFinalAmount(), couponUsed, pointUsed);

            order.fail();
            orderRepository.save(order);

            throw e;
        }
    }

    /**
     * Saga 보상 트랜잭션을 실행합니다.
     * <p>
     * 성공한 단계들을 역순으로 롤백하여 데이터 일관성을 유지합니다.
     *
     * @param processedProductIds 재고가 차감된 상품 ID 목록
     * @param orderItems 주문 항목 (수량 정보 포함)
     * @param userCouponId 사용자 쿠폰 ID
     * @param userId 사용자 ID
     * @param pointAmount 포인트 금액
     * @param couponUsed 쿠폰이 사용되었는지 여부
     * @param pointUsed 포인트가 차감되었는지 여부
     */
    private void compensate(List<Long> processedProductIds, List<OrderItem> orderItems,
                           Long userCouponId, Long userId, Long pointAmount,
                           boolean couponUsed, boolean pointUsed) {
        log.info("보상 트랜잭션 시작");

        // Step 3 롤백: 포인트 복구
        if (pointUsed) {
            try {
                log.info("포인트 복구 시작: userId={}, amount={}", userId, pointAmount);
                userPointService.refundPoint(userId, pointAmount);
            } catch (Exception e) {
                log.error("포인트 복구 실패: userId={}, error={}", userId, e.getMessage(), e);
            }
        }

        // Step 2 롤백: 쿠폰 복구
        if (couponUsed && userCouponId != null) {
            try {
                log.info("쿠폰 복구 시작: userCouponId={}", userCouponId);
                userCouponService.cancelUseCoupon(userCouponId);
            } catch (Exception e) {
                log.error("쿠폰 복구 실패: userCouponId={}, error={}", userCouponId, e.getMessage(), e);
            }
        }

        // Step 1 롤백: 재고 복구
        for (Long productId : processedProductIds) {
            try {
                OrderItem item = orderItems.stream()
                        .filter(oi -> oi.getProductId().equals(productId))
                        .findFirst()
                        .orElseThrow();

                log.info("재고 복구 시작: productId={}, quantity={}", productId, item.getQuantity());
                productStockService.increaseStock(productId, item.getQuantity());
            } catch (Exception e) {
                log.error("재고 복구 실패: productId={}, error={}", productId, e.getMessage(), e);
            }
        }

        log.info("보상 트랜잭션 완료");
    }

    /**
     * 인기 상품 점수를 업데이트합니다.
     * <p>
     * Redis 캐시 업데이트는 부가 기능이므로 실패해도 결제에 영향을 주지 않습니다.
     * 별도의 try-catch로 감싸서 예외를 격리합니다.
     *
     * @param orderItems 구매된 상품 목록
     */
    @Async("asyncExecutor")
    public void updatePopularityScore(List<OrderItem> orderItems) {
        try {
            for (OrderItem item : orderItems) {
                popularProductCacheService.incrementPurchaseScore(item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            // Redis 업데이트 실패는 로그만 남기고 무시
            log.error("인기 상품 점수 업데이트 실패: {}", e.getMessage(), e);
        }
    }
}
