package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.CouponService;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponResponse>> getCoupons() {
        return ResponseEntity.ok(couponService.getCoupons());
    }

    @GetMapping("/coupons/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(
            @PathVariable Long couponId
    ) {
        return ResponseEntity.ok(couponService.getCoupon(couponId));
    }

    @PostMapping("/users/{userId}/coupons")
    public ResponseEntity<IssueCouponResponse> issueCoupon(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        couponService.issueCoupon(userId, request);
        return ResponseEntity.accepted().body(
            new IssueCouponResponse("쿠폰 발급 요청이 접수되었습니다. 잠시 후 발급됩니다.")
        );
    }

    @GetMapping("/users/{userId}/coupons")
    public ResponseEntity<List<UserCouponResponse>> getUserCoupons(
            @PathVariable("userId") Long userId
    ) {
        return ResponseEntity.ok(couponService.getUserCoupons(userId));
    }
}
