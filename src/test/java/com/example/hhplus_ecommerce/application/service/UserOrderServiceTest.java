package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.domain.model.OrderItem;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.OrderItemRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.OrderRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.OrderErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserOrderService userOrderService;

    @Test
    @DisplayName("사용자 주문 목록 조회 성공")
    void getUserOrders_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().point(0L).build();
        Order order1 = Order.builder()
                .userId(userId)
                .totalAmount(20000L)
                .discountAmount(0L)
                .ordererName("홍길동")
                .deliveryAddress("서울시")
                .build();
        Order order2 = Order.builder()
                .userId(userId)
                .totalAmount(30000L)
                .discountAmount(3000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시")
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(orderRepository.findByUserId(userId)).willReturn(List.of(order1, order2));

        // when
        List<UserOrderResponse> result = userOrderService.getUserOrders(userId);

        // then
        assertThat(result).hasSize(2);
        verify(userRepository).findByIdOrThrow(userId);
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("사용자 주문 목록 조회 실패 - 사용자 없음")
    void getUserOrders_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findByIdOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrders(userId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(orderRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 성공")
    void getUserOrder_Success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        User user = User.builder().point(0L).build();
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(20000L)
                .discountAmount(0L)
                .ordererName("홍길동")
                .deliveryAddress("서울시")
                .build();
        OrderItem orderItem = OrderItem.builder()
                .orderId(orderId)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));

        // when
        OrderResponse result = userOrderService.getUserOrder(userId, orderId);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findByIdOrThrow(userId);
        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 실패 - 사용자 없음")
    void getUserOrder_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        Long orderId = 1L;
        given(userRepository.findByIdOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrder(userId, orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(orderRepository, never()).findByIdOrThrow(any());
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 실패 - 주문 없음")
    void getUserOrder_Fail_OrderNotFound() {
        // given
        Long userId = 1L;
        Long orderId = 999L;
        User user = User.builder().point(0L).build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(orderRepository.findByIdOrThrow(orderId))
                .willThrow(new NotFoundException(OrderErrorCode.ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrder(userId, orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(orderRepository).findByIdOrThrow(orderId);
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 실패 - 다른 사용자의 주문")
    void getUserOrder_Fail_NotUserOrder() {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        User user = User.builder().point(0L).build();
        Order order = Order.builder()
                .userId(2L) // 다른 사용자의 주문
                .totalAmount(20000L)
                .discountAmount(0L)
                .ordererName("김철수")
                .deliveryAddress("부산시")
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrder(userId, orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository, never()).findByOrderId(any());
    }
}