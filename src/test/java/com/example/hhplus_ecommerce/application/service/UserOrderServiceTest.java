package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.domain.model.OrderItem;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.repository.OrderItemRepository;
import com.example.hhplus_ecommerce.domain.repository.OrderRepository;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
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
        User user = new User();
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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByUserId(userId)).willReturn(List.of(order1, order2));

        // when
        List<UserOrderResponse> result = userOrderService.getUserOrders(userId);

        // then
        assertThat(result).hasSize(2);
        verify(userRepository).findById(userId);
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("사용자 주문 목록 조회 실패 - 사용자 없음")
    void getUserOrders_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrders(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(orderRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 성공")
    void getUserOrder_Success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        User user = new User();
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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));

        // when
        OrderResponse result = userOrderService.getUserOrder(userId, orderId);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(orderRepository).findById(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 실패 - 사용자 없음")
    void getUserOrder_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        Long orderId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrder(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(orderRepository, never()).findById(any());
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 실패 - 주문 없음")
    void getUserOrder_Fail_OrderNotFound() {
        // given
        Long userId = 1L;
        Long orderId = 999L;
        User user = new User();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrder(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("사용자 주문 상세 조회 실패 - 다른 사용자의 주문")
    void getUserOrder_Fail_NotUserOrder() {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        User user = new User();
        Order order = Order.builder()
                .userId(2L) // 다른 사용자의 주문
                .totalAmount(20000L)
                .discountAmount(0L)
                .ordererName("김철수")
                .deliveryAddress("부산시")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> userOrderService.getUserOrder(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(orderRepository).findById(orderId);
        verify(orderItemRepository, never()).findByOrderId(any());
    }
}