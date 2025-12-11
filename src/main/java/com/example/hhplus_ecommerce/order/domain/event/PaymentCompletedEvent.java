package com.example.hhplus_ecommerce.order.domain.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문이 데이터 플랫폼으로 전송되는 이벤트
 * <p>
 * 결제 완료 후 주문 정보를 외부 데이터 플랫폼에 전송하기 위한 이벤트입니다.
 * 트랜잭션과 외부 API 호출을 분리하여 관심사를 명확히 합니다.
 */
public record PaymentCompletedEvent(
        Long orderId,
        Long userId,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        String ordererName,
        String deliveryAddress,
        List<OrderItemData> orderItems,
        LocalDateTime createdAt
) {
    /**
     * 주문 항목 정보
     */
    public record OrderItemData(
            Long productId,
            String productName,
            Long price,
            Integer quantity
    ) {}
}