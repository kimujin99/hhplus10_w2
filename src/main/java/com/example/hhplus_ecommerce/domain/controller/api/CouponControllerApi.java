package com.example.hhplus_ecommerce.domain.controller.api;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.CouponResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.UserCouponResponse;
import com.example.hhplus_ecommerce.domain.dto.request.IssueCouponRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "쿠폰", description = "쿠폰 관련 API")
public interface CouponControllerApi {

    @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 쿠폰 목록을 조회합니다.")
    ResponseEntity<ApiResponse<List<CouponResponse>>> getAvailableCoupons(HttpServletRequest request);

    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다. 선착순으로 인당 하나씩만 발급 가능합니다.")
    ResponseEntity<ApiResponse<Void>> issueCoupon(
            @PathVariable Long userId,
            @RequestBody IssueCouponRequest issueCouponRequest,
            HttpServletRequest request
    );

    @Operation(summary = "사용자 쿠폰 목록 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
    ResponseEntity<ApiResponse<List<UserCouponResponse>>> getUserCoupons(@PathVariable Long userId, HttpServletRequest request);
}