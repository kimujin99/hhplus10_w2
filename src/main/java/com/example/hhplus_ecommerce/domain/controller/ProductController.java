package com.example.hhplus_ecommerce.domain.controller;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.controller.api.ProductControllerApi;
import com.example.hhplus_ecommerce.domain.dto.reponse.PopularProductResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.ProductResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.StockResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerApi {

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(HttpServletRequest request) {
        List<ProductResponse> products = List.of(
                ProductResponse.builder()
                        .productId(1L)
                        .productName("상품명")
                        .description("상세한 상품 설명...")
                        .price(29900L)
                        .stockQuantity(100)
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), products)
        );
    }

    @GetMapping("/{productId}")
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long productId,
            HttpServletRequest request
    ) {
        ProductResponse product = ProductResponse.builder()
                .productId(productId)
                .productName("상품명")
                .description("상세한 상품 설명...")
                .price(29900L)
                .stockQuantity(100)
                .build();
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), product)
        );
    }

    @GetMapping("/{productId}/stock")
    @Override
    public ResponseEntity<ApiResponse<StockResponse>> getProductStock(
            @PathVariable Long productId,
            HttpServletRequest request
    ) {
        StockResponse stock = StockResponse.builder()
                .productId(productId)
                .productName("상품명")
                .stockQuantity(100)
                .build();
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), stock)
        );
    }

    @GetMapping("/popular")
    @Override
    public ResponseEntity<ApiResponse<List<PopularProductResponse>>> getPopularProducts(HttpServletRequest request) {
        List<PopularProductResponse> popularProducts = List.of(
                PopularProductResponse.builder()
                        .rank(1)
                        .productId(1L)
                        .productName("인기 상품 1")
                        .totalOrderQuantity(150)
                        .orderCount(45)
                        .build(),
                PopularProductResponse.builder()
                        .rank(2)
                        .productId(5L)
                        .productName("인기 상품 2")
                        .totalOrderQuantity(120)
                        .orderCount(38)
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), popularProducts)
        );
    }
}