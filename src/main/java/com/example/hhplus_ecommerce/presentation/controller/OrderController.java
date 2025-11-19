package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.MakeOrderService;
import com.example.hhplus_ecommerce.application.service.MakePaymentService;
import com.example.hhplus_ecommerce.application.service.UserOrderService;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<OrderResponse> makeOrder (
            @Valid @RequestBody OrderRequest orderRequest
    ) {
        return ResponseEntity.ok(makeOrderService.execute(orderRequest));
    }

    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<List<UserOrderResponse>> getUserOrders(
            @PathVariable("userId") Long userId
    ) {
        return ResponseEntity.ok(userOrderService.getUserOrders(userId));
    }

    @GetMapping("/users/{userId}/orders/{orderId}")
    public ResponseEntity<OrderResponse> getUserOrder(
            @PathVariable("userId") Long userId,
            @PathVariable("orderId") Long orderId
    ) {
        return ResponseEntity.ok(userOrderService.getUserOrder(userId, orderId));
    }

    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> makePayment (
            @PathVariable("orderId") Long orderId
    ) {
        return ResponseEntity.ok(makePaymentService.execute(orderId));
    }
}
