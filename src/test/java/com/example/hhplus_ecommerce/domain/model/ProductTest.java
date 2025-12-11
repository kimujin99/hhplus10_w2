package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.common.presentation.exception.BaseException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("재고 차감 성공")
    void subStockQuantity_Success() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(10)
                .build();

        // when
        product.subStockQuantity(5);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void subStockQuantity_Fail_InsufficientStock() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(5)
                .build();

        // when & then
        assertThatThrownBy(() -> product.subStockQuantity(10))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);
    }


    @Test
    @DisplayName("재고 검증 성공")
    void hasSufficientStock_Success() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(10)
                .build();

        // when & then
        assertAll(
                () -> assertTrue(product.hasSufficientStock(10)),
                () -> assertFalse(product.hasSufficientStock(11))
        );
    }

    @Test
    @DisplayName("재고 판매수 조회 검증")
    void getPurchaseCount() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .originalStockQuantity(20)
                .stockQuantity(20)
                .build();

        // when
        product.subStockQuantity(15);

        // then
        assertThat(product.getPurchaseCount()).isEqualTo(15);
    }

    @Test
    @DisplayName("조회수 증가")
    void incrementViewCount() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(0)
                .build();

        // when
        product.incrementViewCount();
        product.incrementViewCount();

        // then
        assertThat(product.getViewCount()).isEqualTo(2);
    }
}