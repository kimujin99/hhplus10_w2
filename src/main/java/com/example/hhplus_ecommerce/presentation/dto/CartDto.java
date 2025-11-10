package com.example.hhplus_ecommerce.presentation.dto;

import com.example.hhplus_ecommerce.domain.model.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class CartDto {

    public record CartItemResponse(
            Long cartItemId,
            Long userId,
            Long productId,
            String productName,
            Long price,
            Integer quantity,
            Long subtotal
    ) {
        public static CartItemResponse from(CartItem cartItem) {
            return new CartItemResponse(
                    cartItem.getId(),
                    cartItem.getUserId(),
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getPrice(),
                    cartItem.getQuantity(),
                    cartItem.getPrice() * cartItem.getQuantity()
            );
        }

        public static List<CartItemResponse> fromList(List<CartItem> cartItems) {
            return cartItems.stream()
                    .map(CartItemResponse::from)
                    .toList();
        }
    }

    public record AddCartItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,

            @NotNull(message = "상품 수량은 필수입니다.")
            @Min(value = 1, message = "상품 수량은 하나 이상이어야 합니다.")
            Integer quantity
    ) {
    }
}