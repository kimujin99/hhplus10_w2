package com.example.hhplus_ecommerce.domain.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장바구니 항목 응답")
public class CartItemResponse {
    @Schema(description = "장바구니 항목 ID", example = "1")
    private Long cartItemId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "상품명")
    private String productName;

    @Schema(description = "상품 가격", example = "29900")
    private Long price;

    @Schema(description = "재고 수량", example = "100")
    private Integer stockQuantity;

    @Schema(description = "장바구니 수량", example = "2")
    private Integer quantity;

    @Schema(description = "소계", example = "59800")
    private Long subtotal;
}