package com.example.hhplus_ecommerce.domain.controller.api;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.PointHistoryResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.PointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "포인트", description = "포인트 관련 API")
public interface PointControllerApi {

    @Operation(summary = "포인트 조회", description = "사용자의 현재 포인트 잔액을 조회합니다.")
    ResponseEntity<ApiResponse<PointResponse>> getPoints(@PathVariable Long userId, HttpServletRequest request);

    @Operation(summary = "포인트 사용 내역 조회", description = "사용자의 포인트 거래 내역을 조회합니다.")
    ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getPointHistory(@PathVariable Long userId, HttpServletRequest request);
}