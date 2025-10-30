package com.example.hhplus_ecommerce.domain.controller.api;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.CartItemResponse;
import com.example.hhplus_ecommerce.domain.dto.request.AddCartRequest;
import com.example.hhplus_ecommerce.domain.dto.request.UpdateCartRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "장바구니", description = "장바구니 관련 API")
public interface CartControllerApi {

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니를 조회합니다.")
    ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart(@PathVariable Long userId, HttpServletRequest request);

    @Operation(summary = "장바구니에 상품 추가", description = "장바구니에 상품을 추가합니다.")
    ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
            @PathVariable Long userId,
            @RequestBody AddCartRequest addCartItemRequest,
            HttpServletRequest request
    );

    @Operation(summary = "장바구니 상품 수량 변경", description = "장바구니 상품의 수량을 변경합니다.")
    ResponseEntity<ApiResponse<CartItemResponse>> updateCartItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartRequest updateQuantityRequest,
            HttpServletRequest request
    );

    @Operation(summary = "장바구니 상품 삭제", description = "장바구니에서 상품을 삭제합니다.")
    ResponseEntity<Void> deleteCartItem(@PathVariable Long userId, @PathVariable Long cartItemId);
}