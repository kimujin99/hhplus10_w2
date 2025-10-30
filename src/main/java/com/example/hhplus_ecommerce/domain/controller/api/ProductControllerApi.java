package com.example.hhplus_ecommerce.domain.controller.api;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.PopularProductResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.ProductResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "상품", description = "상품 관련 API")
public interface ProductControllerApi {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
    ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(HttpServletRequest request);

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
    ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId, HttpServletRequest request);

    @Operation(summary = "상품 재고 조회", description = "상품의 실시간 재고를 조회합니다.")
    ResponseEntity<ApiResponse<StockResponse>> getProductStock(@PathVariable Long productId, HttpServletRequest request);

    @Operation(summary = "인기 상품 통계 조회", description = "최근 3일간의 인기 상품 상위 5개를 조회합니다.")
    ResponseEntity<ApiResponse<List<PopularProductResponse>>> getPopularProducts(HttpServletRequest request);
}