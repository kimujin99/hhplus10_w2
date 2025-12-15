package com.example.hhplus_ecommerce.cart.presentation.controller;

import com.example.hhplus_ecommerce.cart.application.CartService;
import com.example.hhplus_ecommerce.cart.presentation.dto.CartDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/users/{userId}/cart")
    public ResponseEntity<List<CartItemResponse>> getUserCart(
            @PathVariable("userId") Long userId
    ) {
        return ResponseEntity.ok(cartService.getUserCart(userId));
    }

    @PostMapping("/users/{userId}/cart")
    public ResponseEntity<CartItemResponse> addCartItem(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ResponseEntity.ok(cartService.addCartItem(userId, request));
    }

    @DeleteMapping("/cart/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable("cartItemId") Long cartItemId
    ) {
        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }
}
