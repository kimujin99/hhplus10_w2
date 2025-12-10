package com.example.hhplus_ecommerce.order.application;

import com.example.hhplus_ecommerce.order.domain.Order;
import com.example.hhplus_ecommerce.order.domain.OrderItem;
import com.example.hhplus_ecommerce.order.infrastructure.OrderItemRepository;
import com.example.hhplus_ecommerce.order.infrastructure.OrderRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.common.presentation.errorCode.OrderErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import com.example.hhplus_ecommerce.order.presentation.dto.OrderDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public List<UserOrderResponse> getUserOrders(Long userId) {
        userRepository.findByIdOrThrow(userId);

        List<Order> orders = orderRepository.findByUserId(userId);
        return UserOrderResponse.fromList(orders);
    }

    public OrderResponse getUserOrder(Long userId, Long orderId) {
        userRepository.findByIdOrThrow(userId);
        Order order = orderRepository.findByIdOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return OrderResponse.from(order, orderItems);
    }
}