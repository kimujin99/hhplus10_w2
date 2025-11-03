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
@Schema(description = "인기 상품 응답")
public class PopularProductResponse {
    @Schema(description = "순위", example = "1")
    private Integer rank;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "인기 상품 1")
    private String productName;

    @Schema(description = "총 주문 수량", example = "150")
    private Integer totalOrderQuantity;

    @Schema(description = "주문 건수", example = "45")
    private Integer orderCount;
}