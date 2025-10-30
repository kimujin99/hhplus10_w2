package com.example.hhplus_ecommerce.domain.controller;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.controller.api.CouponControllerApi;
import com.example.hhplus_ecommerce.domain.dto.reponse.CouponResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.UserCouponResponse;
import com.example.hhplus_ecommerce.domain.dto.request.IssueCouponRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CouponController implements CouponControllerApi {

    @GetMapping("/coupons")
    @Override
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAvailableCoupons(HttpServletRequest request) {
        List<CouponResponse> coupons = List.of(
                CouponResponse.builder()
                        .couponId(1L)
                        .name("신규 가입 쿠폰")
                        .discountType("FIXED")
                        .discountValue(5000L)
                        .totalQuantity(100)
                        .issuedQuantity(45)
                        .remainingQuantity(55)
                        .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                        .validUntil(LocalDateTime.of(2024, 12, 31, 23, 59))
                        .build(),
                CouponResponse.builder()
                        .couponId(2L)
                        .name("10% 할인 쿠폰")
                        .discountType("PERCENTAGE")
                        .discountValue(10L)
                        .totalQuantity(50)
                        .issuedQuantity(50)
                        .remainingQuantity(0)
                        .validFrom(LocalDateTime.of(2024, 1, 15, 0, 0))
                        .validUntil(LocalDateTime.of(2024, 1, 31, 23, 59))
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), coupons)
        );
    }

    @PostMapping("/users/{userId}/coupons")
    @Override
    public ResponseEntity<ApiResponse<Void>> issueCoupon(
            @PathVariable Long userId,
            @RequestBody IssueCouponRequest issueCouponRequest,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(request.getRequestURI(), null));
    }

    @GetMapping("/users/{userId}/coupons")
    @Override
    public ResponseEntity<ApiResponse<List<UserCouponResponse>>> getUserCoupons(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        List<UserCouponResponse> userCoupons = List.of(
                UserCouponResponse.builder()
                        .userCouponId(1L)
                        .userId(userId)
                        .couponId(1L)
                        .name("신규 가입 쿠폰")
                        .discountType("FIXED")
                        .discountValue(5000L)
                        .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                        .validUntil(LocalDateTime.of(2024, 12, 31, 23, 59))
                        .status("ISSUED")
                        .issuedAt(LocalDateTime.now().minusDays(10))
                        .usedAt(null)
                        .build(),
                UserCouponResponse.builder()
                        .userCouponId(2L)
                        .userId(userId)
                        .couponId(2L)
                        .name("10% 할인 쿠폰")
                        .discountType("PERCENTAGE")
                        .discountValue(10L)
                        .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                        .validUntil(LocalDateTime.of(2024, 12, 31, 23, 59))
                        .status("USED")
                        .issuedAt(LocalDateTime.now().minusDays(5))
                        .usedAt(LocalDateTime.now().minusDays(2))
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), userCoupons)
        );
    }
}