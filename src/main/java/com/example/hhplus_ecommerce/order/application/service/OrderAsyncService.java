package com.example.hhplus_ecommerce.order.application.service;

import com.example.hhplus_ecommerce.order.domain.event.PaymentCompletedEvent;
import com.example.hhplus_ecommerce.order.domain.model.OrderItem;
import com.example.hhplus_ecommerce.product.application.PopularProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 주문 관련 비동기 작업 서비스
 * <p>
 * @Async 어노테이션을 사용하는 메서드들을 별도 클래스로 분리하여
 * 스프링 프록시 우회 문제를 해결합니다.
 * (같은 클래스 내 내부 호출로 인한 프록시 우회 방지)
 * <p>
 * MakePaymentUseCase의 execute() 메서드가 이 서비스의 메서드를 호출하면
 * 스프링 프록시를 통해 @Async가 정상 작동합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAsyncService {

    private final PopularProductCacheService popularProductCacheService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 인기 상품 점수를 비동기로 업데이트합니다.
     * <p>
     * Redis 캐시 업데이트는 부가 기능이므로 실패해도 결제에 영향을 주지 않습니다.
     * 별도의 스레드에서 실행되어 결제 응답 시간에 영향을 주지 않습니다.
     *
     * @param orderItems 구매된 상품 목록
     */
    @Async("asyncExecutor")
    public void updatePopularityScore(List<OrderItem> orderItems) {
        try {
            for (OrderItem item : orderItems) {
                popularProductCacheService.incrementPurchaseScore(
                    item.getProductId(),
                    item.getQuantity()
                );
            }
            log.info("인기 상품 점수 업데이트 완료: itemCount={}", orderItems.size());
        } catch (Exception e) {
            log.error("인기 상품 점수 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 주문 플랫폼 전송 이벤트를 비동기로 발행합니다.
     * <p>
     * Outbox 패턴으로 이벤트를 신뢰성 있게 전송합니다.
     * 별도의 스레드에서 실행되며, 실패 시에도 주문 상태에 영향을 주지 않습니다.
     *
     * @param event 주문 플랫폼 전송 이벤트
     */
    @Async("asyncExecutor")
    public void publishOrderPlatformSentEvent(PaymentCompletedEvent event) {
        try {
            eventPublisher.publishEvent(event);
            log.info("주문 플랫폼 전송 이벤트 발행 완료: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("주문 플랫폼 전송 이벤트 발행 실패: orderId={}, error={}",
                event.orderId(), e.getMessage(), e);
        }
    }
}