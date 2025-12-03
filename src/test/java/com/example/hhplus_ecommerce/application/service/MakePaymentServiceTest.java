package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.application.usecase.MakePaymentUseCase;
import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.domain.model.OrderItem;
import com.example.hhplus_ecommerce.infrastructure.repository.OrderItemRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.OrderRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.OrderErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.PointErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ProductStockService productStockService;

    @Mock
    private UserPointService userPointService;

    @InjectMocks
    private MakePaymentUseCase makePaymentUseCase;

    @Test
    @DisplayName("결제 성공")
    void execute_Success() {
        // given
        Long orderId = 1L;
        Long userId = 1L;

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

        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // when
        PaymentResponse result = makePaymentUseCase.execute(orderId);

        // then
        assertThat(result).isNotNull();

        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
        verify(productStockService).decreaseStock(1L, 2);
        verify(userPointService).usePoint(userId, orderId, 20000L);
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
        assertThatThrownBy(() -> makePaymentUseCase.execute(orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("결제 실패 - 포인트 부족")
    void execute_Fail_InsufficientPoint() {
        // given
        Long orderId = 1L;
        Long userId = 1L;

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

        given(orderRepository.findByIdOrThrow(orderId)).willReturn(order);
        given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem));
        doThrow(new ConflictException(PointErrorCode.INSUFFICIENT_POINT))
                .when(userPointService).usePoint(userId, orderId, 20000L);
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // when & then
        assertThatThrownBy(() -> makePaymentUseCase.execute(orderId))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INSUFFICIENT_POINT);

        verify(orderRepository).findByIdOrThrow(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
        verify(productStockService).decreaseStock(1L, 2);
        verify(userPointService).usePoint(userId, orderId, 20000L);
        // 보상 트랜잭션 실행 확인
        verify(productStockService).increaseStock(1L, 2);
        verify(orderRepository).save(order); // order.fail() 저장
    }
}