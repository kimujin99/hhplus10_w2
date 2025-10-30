package com.example.hhplus_ecommerce.domain.controller.api;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderDetailResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderResponse;
import com.example.hhplus_ecommerce.domain.dto.request.CreateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "주문", description = "주문 관련 API")
public interface OrderControllerApi {

    @Operation(summary = "주문 생성", description = "장바구니의 상품으로 주문을 생성합니다. 주문 시 재고와 쿠폰이 차감됩니다.")
    ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @RequestBody CreateOrderRequest createOrderRequest,
            HttpServletRequest request
    );

    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 목록을 조회합니다.")
    ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@PathVariable Long userId, HttpServletRequest request);

    @Operation(summary = "주문 상세 조회", description = "주문의 상세 정보를 조회합니다.")
    ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId,
            HttpServletRequest request
    );
}