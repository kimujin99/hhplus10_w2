package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.CartService;
import com.example.hhplus_ecommerce.presentation.common.response.ApiResponse;
import com.example.hhplus_ecommerce.presentation.dto.CartDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/users/{userId}/cart")
    public ApiResponse<List<CartItemResponse>> getUserCart(
            @PathVariable("userId") Long userId
    ) {
        return ApiResponse.success(cartService.getUserCart(userId));
    }

    @PostMapping("/users/{userId}/cart")
    public ApiResponse<CartItemResponse> addCartItem(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ApiResponse.success(cartService.addCartItem(userId, request));
    }

    @DeleteMapping("/cart/{cartItemId}")
    public ApiResponse<Void> deleteCartItem(
            @PathVariable("cartItemId") Long cartItemId
    ) {
        cartService.deleteCartItem(cartItemId);
        return ApiResponse.success(null);
    }
}
