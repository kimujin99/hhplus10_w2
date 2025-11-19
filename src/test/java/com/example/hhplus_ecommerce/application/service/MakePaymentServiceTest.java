package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.infrastructure.repository.*;
import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
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
        User user = User.builder().point(0L).build();
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
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build();

        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));
        given(productRepository.findByIdWithLockOrThrow(1L)).willReturn(product);
        given(productRepository.save(any(Product.class))).willReturn(product);
        given(userRepository.findByIdWithLockOrThrow(userId)).willReturn(user);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // when
        PaymentResponse result = makePaymentService.execute(orderId);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(user.getPoint()).isEqualTo(30000L)
        );

        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
        verify(productRepository).findByIdWithLockOrThrow(1L);
        verify(productRepository).save(product);
        verify(userRepository).save(user);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("결제 실패 - 주문 없음")
    void execute_Fail_OrderNotFound() {
        // given
        Long orderId = 999L;
        given(orderRepository.findByIdOrThrow(orderId))
                .willThrow(new NotFoundException(OrderErrorCode.ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> makePaymentService.execute(orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository, never()).findByOrderId(any());
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

        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);
        given(userRepository.findByIdWithLockOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> makePaymentService.execute(orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(orderRepository).findByIdOrThrow(orderId);
        verify(userRepository).findByIdWithLockOrThrow(userId);
    }

    @Test
    @DisplayName("결제 실패 - 포인트 부족")
    void execute_Fail_InsufficientPoint() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        User user = User.builder().point(0L).build();
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
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build();

        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);
        given(userRepository.findByIdWithLockOrThrow(userId)).willReturn(user);
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));
        given(productRepository.findByIdWithLockOrThrow(1L)).willReturn(product);
        given(productRepository.save(any(Product.class))).willReturn(product);

        // when & then
        assertThatThrownBy(() -> makePaymentService.execute(orderId))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INSUFFICIENT_POINT);
        verify(orderRepository).findByIdOrThrow(orderId);
        verify(userRepository).findByIdWithLockOrThrow(userId);
        verify(orderItemRepository).findByOrderId(orderId);
    }
}