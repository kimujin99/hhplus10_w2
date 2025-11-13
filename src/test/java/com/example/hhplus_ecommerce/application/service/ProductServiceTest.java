package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.*;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("전체 상품 목록 조회 성공")
    void getProducts_Success() {
        // given
        Product product1 = Product.builder()
                .productName("상품1")
                .description("설명1")
                .price(10000L)
                .stockQuantity(10)
                .build();
        Product product2 = Product.builder()
                .productName("상품2")
                .description("설명2")
                .price(20000L)
                .stockQuantity(20)
                .build();

        given(productRepository.findAll()).willReturn(List.of(product1, product2));

        // when
        List<ProductResponse> result = productService.getProducts();

        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).productName()).isEqualTo("상품1"),
                () -> assertThat(result.get(1).productName()).isEqualTo("상품2")
        );

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProduct_Success() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(10)
                .build();

        given(productRepository.findByIdOrThrow(productId)).willReturn(product);
        doNothing().when(productRepository).incrementViewCount(productId);

        // when
        ProductResponse result = productService.getProduct(productId);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.productName()).isEqualTo("테스트 상품")
        );

        verify(productRepository).findByIdOrThrow(productId);
        verify(productRepository).incrementViewCount(productId);
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품")
    void getProduct_Fail_ProductNotFound() {
        // given
        Long productId = 999L;
        given(productRepository.findByIdOrThrow(productId))
                .willThrow(new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
        verify(productRepository).findByIdOrThrow(productId);
        verify(productRepository, never()).incrementViewCount(any());
    }

    @Test
    @DisplayName("상품 재고 조회 성공")
    void getProductStock_Success() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .stockQuantity(50)
                .build();

        given(productRepository.findByIdOrThrow(productId)).willReturn(product);

        // when
        ProductStockResponse result = productService.getProductStock(productId);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.stockQuantity()).isEqualTo(50)
        );

        verify(productRepository).findByIdOrThrow(productId);
    }

    @Test
    @DisplayName("상품 재고 조회 실패 - 존재하지 않는 상품")
    void getProductStock_Fail_ProductNotFound() {
        // given
        Long productId = 999L;
        given(productRepository.findByIdOrThrow(productId))
                .willThrow(new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> productService.getProductStock(productId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
        verify(productRepository).findByIdOrThrow(productId);
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() {
        // given
        Product product1 = Product.builder()
                .productName("인기 상품1")
                .description("설명1")
                .price(10000L)
                .originalStockQuantity(20)
                .stockQuantity(10)
                .build();
        product1.incrementViewCount();
        product1.incrementViewCount();

        Product product2 = Product.builder()
                .productName("인기 상품2")
                .description("설명2")
                .price(20000L)
                .originalStockQuantity(20)
                .stockQuantity(20)
                .build();
        product2.incrementViewCount();

        given(productRepository.findPopularProduct()).willReturn(List.of(product2, product1));

        // when
        List<PopularProductResponse> result = productService.getPopularProducts();

        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).productId()).isEqualTo(product2.getId())
        );

        verify(productRepository).findPopularProduct();
    }
}