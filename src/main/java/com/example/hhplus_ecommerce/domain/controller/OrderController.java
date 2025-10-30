package com.example.hhplus_ecommerce.domain.controller;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.controller.api.OrderControllerApi;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderDetailResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderItemResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderResponse;
import com.example.hhplus_ecommerce.domain.dto.request.CreateOrderRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController implements OrderControllerApi {

    @PostMapping("/orders")
    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @RequestBody CreateOrderRequest createOrderRequest,
            HttpServletRequest request
    ) {
        OrderDetailResponse order = OrderDetailResponse.builder()
                .orderId(1L)
                .userId(createOrderRequest.getUserId())
                .totalAmount(59800L)
                .discountAmount(5000L)
                .finalAmount(54800L)
                .status("PENDING")
                .ordererName(createOrderRequest.getOrdererName())
                .deliveryAddress(createOrderRequest.getDeliveryAddress())
                .orderedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userCouponId(createOrderRequest.getUserCouponId())
                .couponName("신규 가입 쿠폰")
                .discountType("FIXED")
                .discountValue(5000L)
                .items(List.of(
                        OrderItemResponse.builder()
                                .orderItemId(1L)
                                .productId(1L)
                                .productName("상품명")
                                .price(29900L)
                                .quantity(2)
                                .subtotal(59800L)
                                .build()
                ))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(request.getRequestURI(), order));
    }

    @GetMapping("/users/{userId}/orders")
    @Override
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        List<OrderResponse> orders = List.of(
                OrderResponse.builder()
                        .orderId(1L)
                        .totalAmount(59800L)
                        .discountAmount(5000L)
                        .finalAmount(54800L)
                        .status("CONFIRMED")
                        .orderedAt(LocalDateTime.now().minusDays(1))
                        .updatedAt(LocalDateTime.now().minusDays(1))
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), orders)
        );
    }

    @GetMapping("/users/{userId}/orders/{orderId}")
    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        OrderDetailResponse order = OrderDetailResponse.builder()
                .orderId(orderId)
                .userId(userId)
                .totalAmount(59800L)
                .discountAmount(5000L)
                .finalAmount(54800L)
                .status("CONFIRMED")
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구 테헤란로 123")
                .orderedAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .userCouponId(1L)
                .couponName("신규 가입 쿠폰")
                .discountType("FIXED")
                .discountValue(5000L)
                .items(List.of(
                        OrderItemResponse.builder()
                                .orderItemId(1L)
                                .productId(1L)
                                .productName("상품명")
                                .price(29900L)
                                .quantity(2)
                                .subtotal(59800L)
                                .build()
                ))
                .build();
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), order)
        );
    }
}