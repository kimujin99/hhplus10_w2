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
@Schema(description = "포인트 충전 요청")
public class ChargePointRequest {
    @Schema(description = "충전 금액", example = "100000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;
}