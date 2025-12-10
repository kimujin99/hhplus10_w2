package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.cart.model.CartItem;
import com.example.hhplus_ecommerce.common.presentation.exception.BaseException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class CartItemTest {

    @Test
    @DisplayName("장바구니 아이템 수량 변경 성공")
    void updateQuantity_Success() {
        // given
        CartItem cartItem = CartItem.builder()
                .userId(1L)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(5)
                .build();

        // when
        cartItem.updateQuantity(10);

        // then
        assertThat(cartItem.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("장바구니 아이템 수량 변경 실패 - 0 이하")
    void updateQuantity_Fail_ZeroOrNegative() {
        // given
        CartItem cartItem = CartItem.builder()
                .userId(1L)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(5)
                .build();

        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> cartItem.updateQuantity(0))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_QUANTITY),
                () -> assertThatThrownBy(() -> cartItem.updateQuantity(-1))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_QUANTITY)
        );
    }
}