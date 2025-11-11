package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.domain.model.OrderItem;
import com.example.hhplus_ecommerce.domain.repository.OrderItemRepository;
import com.example.hhplus_ecommerce.domain.repository.OrderRepository;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public List<UserOrderResponse> getUserOrders(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Order> orders = orderRepository.findByUserId(userId);
        return UserOrderResponse.fromList(orders);
    }

    public OrderResponse getUserOrder(Long userId, Long orderId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return OrderResponse.from(order, orderItems);
    }
}