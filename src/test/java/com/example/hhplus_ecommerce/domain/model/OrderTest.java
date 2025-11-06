package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    @Test
    @DisplayName("주문 생성 시 상태는 PENDING")
    void createOrder_InitialStatusIsPending() {
        // given & when
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(1000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();

        // then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 확정 성공")
    void confirm_Success() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(1000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();

        // when
        order.confirm();

        // then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("주문 실패 처리 성공")
    void fail_Success() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(1000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();

        // when
        order.fail();

        // then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.FAILED);
    }

    @Test
    @DisplayName("주문 확정 실패 - 이미 확정된 주문")
    void confirm_Fail_AlreadyConfirmed() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(1000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();
        order.confirm();

        // when & then
        assertThatThrownBy(order::confirm)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("주문 실패 처리 실패 - 이미 확정된 주문")
    void fail_Fail_AlreadyConfirmed() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(1000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();
        order.confirm();

        // when & then
        assertThatThrownBy(order::fail)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("최종 금액 계산")
    void getFinalAmount() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(1500L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();

        // when
        Long finalAmount = order.getFinalAmount();

        // then
        assertThat(finalAmount).isEqualTo(8500L);
    }

    @Test
    @DisplayName("최종 금액 계산 - 할인 없음")
    void getFinalAmount_NoDiscount() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .discountAmount(0L)
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구")
                .build();

        // when
        Long finalAmount = order.getFinalAmount();

        // then
        assertThat(finalAmount).isEqualTo(10000L);
    }
}