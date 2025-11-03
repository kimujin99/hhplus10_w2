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
@Schema(description = "쿠폰 발급 요청")
public class IssueCouponRequest {
    @Schema(description = "쿠폰 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long couponId;
}