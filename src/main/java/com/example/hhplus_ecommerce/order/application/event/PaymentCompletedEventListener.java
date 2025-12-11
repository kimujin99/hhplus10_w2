package com.example.hhplus_ecommerce.order.application.event;

import com.example.hhplus_ecommerce.common.presentation.controller.ExternalMockApi;
import com.example.hhplus_ecommerce.order.application.mapper.PaymentCompletedEventMapper;
import com.example.hhplus_ecommerce.order.domain.event.PaymentCompletedEvent;
import com.example.hhplus_ecommerce.order.domain.model.OrderItem;
import com.example.hhplus_ecommerce.product.application.PopularProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * 결제 성공 이벤트 리스너
 * <p>
 * 리스너 1: 상품별로 인기상품 집계 점수 올리기
 * 리스너 2: 외부 플랫폼에 결제 정보 전송
 * 트랜잭션 완료 후 별도의 스레드에서 실행(비동기)되어 결제 프로세스에 영향을 주지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventListener {

    private final PopularProductCacheService popularProductCacheService;
    private final ExternalMockApi externalMockApi;
    private final PaymentCompletedEventMapper eventMapper;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted_UpdatePopularity(PaymentCompletedEvent event) {
        List<PaymentCompletedEvent.OrderItemData> orderItems  = event.orderItems();

        try {
            for (PaymentCompletedEvent.OrderItemData item : orderItems) {
                popularProductCacheService.incrementPurchaseScore(
                        item.productId(),
                        item.quantity()
                );
            }
            log.info("인기 상품 점수 업데이트 완료: itemCount={}", orderItems.size());
        } catch (Exception e) {
            log.error("인기 상품 점수 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted_SendToExternal(PaymentCompletedEvent event) {
        log.info("주문 플랫폼 전송 이벤트 처리 시작: orderId={}, userId={}", event.orderId(), event.userId());

        try {
            Map<String, Object> orderData = eventMapper.toPayload(event);
            externalMockApi.mockReceive(orderData);
            log.info("주문 플랫폼 전송 성공: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("주문 플랫폼 전송 실패: orderId={}, error={}", event.orderId(), e.getMessage(), e);
        }
    }
}
