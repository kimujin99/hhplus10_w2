package com.example.hhplus_ecommerce.order.application.mapper;

import com.example.hhplus_ecommerce.order.domain.event.PaymentCompletedEvent;
import com.example.hhplus_ecommerce.order.domain.model.Order;
import com.example.hhplus_ecommerce.order.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentCompletedEventMapper {
    public PaymentCompletedEvent toEvent(Order order, List<OrderItem> orderItems) {
        return new PaymentCompletedEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getOrdererName(),
                order.getDeliveryAddress(),
                orderItems.stream()
                        .map(this::toOrderItemData)
                        .toList(),
                order.getCreatedAt()
        );
    }

    private PaymentCompletedEvent.OrderItemData toOrderItemData(OrderItem item) {
        return new PaymentCompletedEvent.OrderItemData(
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity()
        );
    }

    public Map<String, Object> toPayload(PaymentCompletedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.orderId());
        payload.put("userId", event.userId());
        payload.put("totalAmount", event.totalAmount());
        payload.put("discountAmount", event.discountAmount());
        payload.put("finalAmount", event.finalAmount());
        payload.put("ordererName", event.ordererName());
        payload.put("deliveryAddress", event.deliveryAddress());
        payload.put("orderItems", event.orderItems());
        payload.put("createdAt", event.createdAt());
        return payload;
    }
}
