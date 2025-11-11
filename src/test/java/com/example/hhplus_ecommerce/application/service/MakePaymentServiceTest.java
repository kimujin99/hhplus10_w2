package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.domain.repository.*;
import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.OrderErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.PointErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MakePaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private MakePaymentService makePaymentService;

    @Test
    @DisplayName("결제 성공")
    void execute_Success() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        User user = new User();
        user.chargePoint(50000L);

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

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));
        given(userRepository.save(any(User.class))).willReturn(user);
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // when
        PaymentResponse result = makePaymentService.execute(orderId);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(user.getPoint()).isEqualTo(30000L)
        );

        verify(orderRepository).findById(orderId);
        verify(userRepository).findById(userId);
        verify(orderItemRepository).findByOrderId(orderId);
        verify(userRepository).save(user);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("결제 실패 - 주문 없음")
    void execute_Fail_OrderNotFound() {
        // given
        Long orderId = 999L;
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> makePaymentService.execute(orderId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        verify(orderRepository).findById(orderId);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("결제 실패 - 사용자 없음")
    void execute_Fail_UserNotFound() {
        // given
        Long orderId = 1L;
        Long userId = 999L;
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(20000L)
                .discountAmount(0L)
                .ordererName("홍길동")
                .deliveryAddress("서울시")
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> makePaymentService.execute(orderId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(orderRepository).findById(orderId);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("결제 실패 - 포인트 부족")
    void execute_Fail_InsufficientPoint() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        User user = new User();
        user.chargePoint(10000L);

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

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));

        // when & then
        assertThatThrownBy(() -> makePaymentService.execute(orderId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INSUFFICIENT_POINT);
        verify(orderRepository).findById(orderId);
        verify(userRepository).findById(userId);
        verify(orderItemRepository).findByOrderId(orderId);
    }
}