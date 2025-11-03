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
@Schema(description = "상품 응답")
public class ProductResponse {
    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "상품명")
    private String productName;

    @Schema(description = "상품 설명", example = "상세한 상품 설명...")
    private String description;

    @Schema(description = "가격", example = "29900")
    private Long price;

    @Schema(description = "재고 수량", example = "100")
    private Integer stockQuantity;
}
