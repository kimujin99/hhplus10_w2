package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.CouponService;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("GET /api/v1/coupons - 쿠폰 목록 조회 성공")
    void getCoupons_Success() throws Exception {
        // given
        List<CouponResponse> coupons = List.of(
                new CouponResponse(
                        1L,
                        "5000원 할인 쿠폰",
                        "FIXED",
                        5000L,
                        100,
                        50,
                        50,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(7)
                ),
                new CouponResponse(
                        2L,
                        "10% 할인 쿠폰",
                        "PERCENTAGE",
                        10L,
                        200,
                        100,
                        100,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(7)
                )
        );
        when(couponService.getCoupons()).thenReturn(coupons);

        // when & then
        mockMvc.perform(get("/api/v1/coupons"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].couponId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("5000원 할인 쿠폰"))
                .andExpect(jsonPath("$.data[1].couponId").value(2))
                .andExpect(jsonPath("$.data[1].name").value("10% 할인 쿠폰"));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/coupons - 쿠폰 발급 성공")
    void issueCoupon_Success() throws Exception {
        // given
        Long userId = 1L;
        IssueCouponRequest request = new IssueCouponRequest(1L);
        UserCouponResponse response = new UserCouponResponse(
                1L,
                userId,
                1L,
                "5000원 할인 쿠폰",
                "FIXED",
                5000L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                "UNUSED",
                LocalDateTime.now()
        );
        when(couponService.issueCoupon(anyLong(), any(IssueCouponRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userCouponId").value(1))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.couponName").value("5000원 할인 쿠폰"))
                .andExpect(jsonPath("$.data.status").value("UNUSED"));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/coupons - 쿠폰 발급 실패 (재고 없음)")
    void issueCoupon_FailNoStock() throws Exception {
        // given
        Long userId = 1L;
        IssueCouponRequest request = new IssueCouponRequest(1L);
        when(couponService.issueCoupon(anyLong(), any(IssueCouponRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.COUPON_SOLD_OUT));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/coupons - 쿠폰 발급 실패 (중복 발급)")
    void issueCoupon_FailDuplicate() throws Exception {
        // given
        Long userId = 1L;
        IssueCouponRequest request = new IssueCouponRequest(1L);
        when(couponService.issueCoupon(anyLong(), any(IssueCouponRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/coupons - 사용자 쿠폰 목록 조회 성공")
    void getUserCoupons_Success() throws Exception {
        // given
        Long userId = 1L;
        List<UserCouponResponse> userCoupons = List.of(
                new UserCouponResponse(
                        1L,
                        userId,
                        1L,
                        "5000원 할인 쿠폰",
                        "FIXED",
                        5000L,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(7),
                        "UNUSED",
                        LocalDateTime.now()
                ),
                new UserCouponResponse(
                        2L,
                        userId,
                        2L,
                        "10% 할인 쿠폰",
                        "PERCENTAGE",
                        10L,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(7),
                        "USED",
                        LocalDateTime.now().minusDays(1)
                )
        );
        when(couponService.getUserCoupons(anyLong())).thenReturn(userCoupons);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(userId))
                .andExpect(jsonPath("$.data[0].status").value("UNUSED"))
                .andExpect(jsonPath("$.data[1].status").value("USED"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/coupons - 사용자 쿠폰 없음")
    void getUserCoupons_Empty() throws Exception {
        // given
        Long userId = 1L;
        when(couponService.getUserCoupons(anyLong())).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/coupons - 사용자 없음")
    void getUserCoupons_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        when(couponService.getUserCoupons(anyLong()))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}