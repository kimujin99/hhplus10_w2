package com.example.hhplus_ecommerce.domain.controller;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.controller.api.PaymentControllerApi;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderDetailResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderItemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/payments")
public class PaymentController implements PaymentControllerApi {

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createPayment(
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        OrderDetailResponse payment = OrderDetailResponse.builder()
                .orderId(orderId)
                .userId(1L)
                .totalAmount(59800L)
                .discountAmount(5000L)
                .finalAmount(54800L)
                .status("CONFIRMED")
                .ordererName("홍길동")
                .deliveryAddress("서울시 강남구 테헤란로 123")
                .orderedAt(LocalDateTime.now().minusMinutes(5))
                .updatedAt(LocalDateTime.now())
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(request.getRequestURI(), payment));
    }
}