package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.UserService;
import com.example.hhplus_ecommerce.presentation.common.response.ApiResponse;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}/points")
    public ApiResponse<PointResponse> getPoint(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.getPoint(userId));
    }

    @GetMapping("/{userId}/points/history")
    public ApiResponse<List<PointHistoryResponse>> getPointHistory(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.getPointHistory(userId));
    }

    @PostMapping("/{userId}/points/charge")
    public ApiResponse<PointResponse> chargePoint(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody ChargePointRequest request
    ) {
        return ApiResponse.success(userService.chargePoint(userId, request));
    }
}
