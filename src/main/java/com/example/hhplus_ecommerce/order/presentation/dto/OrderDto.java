package com.example.hhplus_ecommerce.order.presentation.dto;

import com.example.hhplus_ecommerce.order.domain.Order;
import com.example.hhplus_ecommerce.order.domain.OrderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    public record OrderRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotBlank(message = "주문자명은 필수입니다.")
            String ordererName,

            @NotBlank(message = "배송지는 필수입니다.")
            String deliveryAddress,

            Long userCouponId
    ) {
    }

    public record OrderResponse(
            Long orderId,
            Long userId,
            Long totalAmount,
            Long discountAmount,
            Long finalAmount,
            String status,
            String ordererName,
            String deliveryAddress,
            List<OrderItemResponse> orderItems,
            LocalDateTime createdAt
    ) {
        public static OrderResponse from(Order order, List<OrderItem> orderItems) {
            return new OrderResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getDiscountAmount(),
                    order.getFinalAmount(),
                    order.getStatus().name(),
                    order.getOrdererName(),
                    order.getDeliveryAddress(),
                    OrderItemResponse.fromList(orderItems),
                    order.getCreatedAt()
            );
        }
    }

    public record OrderItemResponse(
            Long orderItemId,
            Long productId,
            String productName,
            Long price,
            Integer quantity,
            Long subtotal
    ) {
        public static OrderItemResponse from(OrderItem orderItem) {
            return new OrderItemResponse(
                    orderItem.getId(),
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getPrice(),
                    orderItem.getQuantity(),
                    orderItem.getSubTotal()
            );
        }

        public static List<OrderItemResponse> fromList(List<OrderItem> orderItems) {
            return orderItems.stream()
                    .map(OrderItemResponse::from)
                    .toList();
        }
    }

    public record UserOrderResponse(
            Long orderId,
            Long totalAmount,
            Long discountAmount,
            Long finalAmount,
            String status,
            LocalDateTime createdAt
    ) {
        public static UserOrderResponse from(Order order) {
            return new UserOrderResponse(
                    order.getId(),
                    order.getTotalAmount(),
                    order.getDiscountAmount(),
                    order.getFinalAmount(),
                    order.getStatus().name(),
                    order.getCreatedAt()
            );
        }

        public static List<UserOrderResponse> fromList(List<Order> orders) {
            return orders.stream()
                    .map(UserOrderResponse::from)
                    .toList();
        }
    }

    public record PaymentResponse(
            Long orderId,
            Long paymentAmount,
            String status,
            LocalDateTime paidAt
    ) {
        public static PaymentResponse from(Order order) {
            return new PaymentResponse(
                    order.getId(),
                    order.getFinalAmount(),
                    order.getStatus().name(),
                    order.getUpdatedAt()
            );
        }
    }
}