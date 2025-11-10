package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.UserService;
import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.*;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/users/{userId}/points - 포인트 조회 성공")
    void getPoint_Success() throws Exception {
        // given
        Long userId = 1L;
        PointResponse response = new PointResponse(userId, 50000L);
        when(userService.getPoint(anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/points", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.point").value(50000));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/points - 포인트 조회 실패 (사용자 없음)")
    void getPoint_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        when(userService.getPoint(anyLong()))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/points", userId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/points/history - 포인트 히스토리 조회 성공")
    void getPointHistory_Success() throws Exception {
        // given
        Long userId = 1L;
        List<PointHistoryResponse> histories = List.of(
                new PointHistoryResponse(1L, userId, null, "CHARGE", 50000L, 50000L, LocalDateTime.now()),
                new PointHistoryResponse(2L, userId, 1L, "USE", 30000L, 20000L, LocalDateTime.now())
        );
        when(userService.getPointHistory(anyLong())).thenReturn(histories);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/points/history", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(userId))
                .andExpect(jsonPath("$.data[0].transactionType").value("CHARGE"))
                .andExpect(jsonPath("$.data[0].amount").value(50000))
                .andExpect(jsonPath("$.data[0].balanceAfter").value(50000))
                .andExpect(jsonPath("$.data[1].transactionType").value("USE"))
                .andExpect(jsonPath("$.data[1].amount").value(30000))
                .andExpect(jsonPath("$.data[1].balanceAfter").value(20000));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/points/history - 빈 히스토리 조회")
    void getPointHistory_Empty() throws Exception {
        // given
        Long userId = 1L;
        when(userService.getPointHistory(anyLong())).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/points/history", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/points/charge - 포인트 충전 성공")
    void chargePoint_Success() throws Exception {
        // given
        Long userId = 1L;
        ChargePointRequest request = new ChargePointRequest(100000L);
        PointResponse response = new PointResponse(userId, 150000L);
        when(userService.chargePoint(anyLong(), any(ChargePointRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.point").value(150000));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/points/charge - 포인트 충전 실패 (사용자 없음)")
    void chargePoint_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        ChargePointRequest request = new ChargePointRequest(100000L);
        when(userService.chargePoint(anyLong(), any(ChargePointRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/points/charge - 포인트 충전 실패 (음수 금액)")
    void chargePoint_NegativeAmount() throws Exception {
        // given
        Long userId = 1L;
        ChargePointRequest request = new ChargePointRequest(-10000L);
        when(userService.chargePoint(anyLong(), any(ChargePointRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}