package com.example.hhplus_ecommerce.domain.controller;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.controller.api.PointControllerApi;
import com.example.hhplus_ecommerce.domain.dto.reponse.PointHistoryResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.PointResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/points")
public class PointController implements PointControllerApi {

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<PointResponse>> getPoints(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        PointResponse point = PointResponse.builder()
                .userId(userId)
                .userName("홍길동")
                .pointBalance(50000L)
                .updatedAt(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), point)
        );
    }

    @GetMapping("/history")
    @Override
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getPointHistory(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        List<PointHistoryResponse> history = List.of(
                PointHistoryResponse.builder()
                        .pointHistoryId(1L)
                        .userId(userId)
                        .orderId(5L)
                        .transactionType("USE")
                        .amount(29900L)
                        .balanceAfter(120100L)
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build(),
                PointHistoryResponse.builder()
                        .pointHistoryId(2L)
                        .userId(userId)
                        .orderId(null)
                        .transactionType("CHARGE")
                        .amount(100000L)
                        .balanceAfter(150000L)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), history)
        );
    }
}