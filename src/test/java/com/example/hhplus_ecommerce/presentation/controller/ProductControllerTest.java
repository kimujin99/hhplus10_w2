package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.ProductService;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/v1/products - 상품 목록 조회 성공")
    void getProducts_Success() throws Exception {
        // given
        List<ProductResponse> products = List.of(
                new ProductResponse(1L, "맥북 프로", "애플 맥북 프로", 2000000L, 10, 100),
                new ProductResponse(2L, "아이패드", "애플 아이패드", 1000000L, 20, 200),
                new ProductResponse(3L, "에어팟", "애플 에어팟", 300000L, 50, 300)
        );
        when(productService.getProducts()).thenReturn(products);

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName").value("맥북 프로"))
                .andExpect(jsonPath("$[0].price").value(2000000))
                .andExpect(jsonPath("$[0].stockQuantity").value(10))
                .andExpect(jsonPath("$[0].viewCount").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/products - 상품 목록 조회 성공 (빈 목록)")
    void getProducts_Empty() throws Exception {
        // given
        when(productService.getProducts()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId} - 특정 상품 조회 성공")
    void getProduct_Success() throws Exception {
        // given
        Long productId = 1L;
        ProductResponse response = new ProductResponse(productId, "맥북 프로", "애플 맥북 프로", 2000000L, 10, 100);
        when(productService.getProduct(anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("맥북 프로"))
                .andExpect(jsonPath("$.description").value("애플 맥북 프로"))
                .andExpect(jsonPath("$.price").value(2000000))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.viewCount").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId} - 상품 조회 실패 (존재하지 않음)")
    void getProduct_NotFound() throws Exception {
        // given
        Long productId = 999L;
        when(productService.getProduct(anyLong()))
                .thenThrow(new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId}/stock - 상품 재고 조회 성공")
    void getProductStock_Success() throws Exception {
        // given
        Long productId = 1L;
        ProductStockResponse response = new ProductStockResponse(productId, 10);
        when(productService.getProductStock(anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}/stock", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId}/stock - 재고 조회 실패 (상품 없음)")
    void getProductStock_NotFound() throws Exception {
        // given
        Long productId = 999L;
        when(productService.getProductStock(anyLong()))
                .thenThrow(new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}/stock", productId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/products/popular - 인기 상품 조회 성공")
    void getPopularProducts_Success() throws Exception {
        // given
        List<PopularProductResponse> popularProducts = List.of(
                new PopularProductResponse(1L, "맥북 프로", 1000),
                new PopularProductResponse(2L, "아이패드", 800),
                new PopularProductResponse(3L, "에어팟", 500)
        );
        when(productService.getPopularProducts()).thenReturn(popularProducts);

        // when & then
        mockMvc.perform(get("/api/v1/products/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName").value("맥북 프로"))
                .andExpect(jsonPath("$[0].viewCount").value(1000));
    }

    @Test
    @DisplayName("GET /api/v1/products/popular - 인기 상품 조회 성공 (빈 목록)")
    void getPopularProducts_Empty() throws Exception {
        // given
        when(productService.getPopularProducts()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/products/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}