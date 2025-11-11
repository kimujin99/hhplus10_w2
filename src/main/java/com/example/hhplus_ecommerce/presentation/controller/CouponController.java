package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.CouponService;
import com.example.hhplus_ecommerce.presentation.common.response.ApiResponse;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/coupons")
    public ApiResponse<List<CouponResponse>> getCoupons() {
        return ApiResponse.success(couponService.getCoupons());
    }

    @PostMapping("/users/{userId}/coupons")
    public ApiResponse<UserCouponResponse> issueCoupon(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        return ApiResponse.success(couponService.issueCoupon(userId, request));
    }

    @GetMapping("/users/{userId}/coupons")
    public ApiResponse<List<UserCouponResponse>> getUserCoupons(
            @PathVariable("userId") Long userId
    ) {
        return ApiResponse.success(couponService.getUserCoupons(userId));
    }
}
