package com.example.hhplus_ecommerce.domain.controller;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.controller.api.CartControllerApi;
import com.example.hhplus_ecommerce.domain.dto.reponse.CartItemResponse;
import com.example.hhplus_ecommerce.domain.dto.request.AddCartRequest;
import com.example.hhplus_ecommerce.domain.dto.request.UpdateCartRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/cart")
@RequiredArgsConstructor
public class CartController implements CartControllerApi {
    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        List<CartItemResponse> cartItems = List.of(
                CartItemResponse.builder()
                        .cartItemId(1L)
                        .userId(userId)
                        .productId(1L)
                        .productName("상품명")
                        .price(29900L)
                        .stockQuantity(100)
                        .quantity(2)
                        .subtotal(59800L)
                        .build()
        );
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), cartItems)
        );
    }

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
            @PathVariable Long userId,
            @RequestBody AddCartRequest addCartItemRequest,
            HttpServletRequest request
    ) {
        CartItemResponse cartItem = CartItemResponse.builder()
                .cartItemId(1L)
                .userId(userId)
                .productId(addCartItemRequest.getProductId())
                .productName("상품명")
                .price(29900L)
                .stockQuantity(100)
                .quantity(addCartItemRequest.getQuantity())
                .subtotal(29900L * addCartItemRequest.getQuantity())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(request.getRequestURI(), cartItem));
    }

    @PatchMapping("/{cartItemId}")
    @Override
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartRequest updateQuantityRequest,
            HttpServletRequest request
    ) {
        CartItemResponse cartItem = CartItemResponse.builder()
                .cartItemId(cartItemId)
                .userId(userId)
                .productId(1L)
                .productName("상품명")
                .price(29900L)
                .stockQuantity(100)
                .quantity(updateQuantityRequest.getQuantity())
                .subtotal(29900L * updateQuantityRequest.getQuantity())
                .build();
        return ResponseEntity.ok(
                ApiResponse.success(request.getRequestURI(), cartItem)
        );
    }

    @DeleteMapping("/{cartItemId}")
    @Override
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId
    ) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}