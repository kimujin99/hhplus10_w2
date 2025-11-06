package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.MakeOrderService;
import com.example.hhplus_ecommerce.application.service.MakePaymentService;
import com.example.hhplus_ecommerce.application.service.UserOrderService;
import com.example.hhplus_ecommerce.presentation.common.ApiResponse;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final MakeOrderService makeOrderService;
    private final MakePaymentService makePaymentService;
    private final UserOrderService userOrderService;

    @PostMapping("/orders")
    public ApiResponse<OrderResponse> makeOrder (
            @Valid @RequestBody OrderRequest orderRequest
    ) {
        return ApiResponse.success(makeOrderService.execute(orderRequest));
    }

    @GetMapping("/users/{userId}/orders")
    public ApiResponse<List<UserOrderResponse>> getUserOrders(
            @PathVariable("userId") Long userId
    ) {
        return ApiResponse.success(userOrderService.getUserOrders(userId));
    }

    @GetMapping("/users/{userId}/orders/{orderId}")
    public ApiResponse<OrderResponse> getUserOrder(
            @PathVariable("userId") Long userId,
            @PathVariable("orderId") Long orderId
    ) {
        return ApiResponse.success(userOrderService.getUserOrder(userId, orderId));
    }

    @PostMapping("/orders/{orderId}/payments")
    public ApiResponse<PaymentResponse> makePayment (
            @PathVariable("orderId") Long orderId
    ) {
        return ApiResponse.success(makePaymentService.execute(orderId));
    }
}
