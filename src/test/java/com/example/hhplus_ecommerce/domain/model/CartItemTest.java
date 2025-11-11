package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
        assertThatThrownBy(() -> cartItem.updateQuantity(0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_QUANTITY);

        assertThatThrownBy(() -> cartItem.updateQuantity(-1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_QUANTITY);
    }
}