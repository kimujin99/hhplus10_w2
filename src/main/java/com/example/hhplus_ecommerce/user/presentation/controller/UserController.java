package com.example.hhplus_ecommerce.user.presentation.controller;

import com.example.hhplus_ecommerce.user.application.UserPointService;
import com.example.hhplus_ecommerce.user.application.UserService;
import com.example.hhplus_ecommerce.user.presentation.dto.UserDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPointService userPointService;

    @GetMapping("/{userId}/points")
    public ResponseEntity<PointResponse> getPoint(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(userService.getPoint(userId));
    }

    @GetMapping("/{userId}/points/history")
    public ResponseEntity<List<PointHistoryResponse>> getPointHistory(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(userService.getPointHistory(userId));
    }

    @PostMapping("/{userId}/points/charge")
    public ResponseEntity<PointResponse> chargePoint(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody ChargePointRequest request
    ) {
        return ResponseEntity.ok(userPointService.chargePoint(userId, request));
    }
}
