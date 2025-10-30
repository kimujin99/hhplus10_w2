package com.example.hhplus_ecommerce.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장바구니 수량 변경 요청")
public class UpdateCartRequest {
    @Schema(description = "수량", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}