package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("재고 추가 성공")
    void addStockQuantity_Success() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(10)
                .build();

        // when
        product.addStockQuantity(5);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(15);
    }

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
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
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
    @DisplayName("재고 인기도 조회 검증")
    void getPopularityScore() {
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
        product.incrementViewCount();
        product.incrementViewCount();
        product.incrementViewCount();

        // then
        double expectedScore = 3 + (15.0 / 20) * 100 * 2;
        assertThat(product.getPopularityScore()).isEqualTo((int)expectedScore);
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

    @Test
    @DisplayName("재고 복합 시나리오")
    void complexScenario() {
        // given
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(100)
                .build();

        // when & then
        product.subStockQuantity(30);
        assertThat(product.getStockQuantity()).isEqualTo(70);

        product.addStockQuantity(20);
        assertThat(product.getStockQuantity()).isEqualTo(90);

        product.subStockQuantity(90);
        assertThat(product.getStockQuantity()).isEqualTo(0);
    }
}