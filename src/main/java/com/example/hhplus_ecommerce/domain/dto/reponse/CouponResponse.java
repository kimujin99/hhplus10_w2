package com.example.hhplus_ecommerce.domain.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "쿠폰 응답")
public class CouponResponse {
    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

    @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
    private String name;

    @Schema(description = "할인 유형 (FIXED, PERCENTAGE)", example = "FIXED")
    private String discountType;

    @Schema(description = "할인 값", example = "5000")
    private Long discountValue;

    @Schema(description = "총 발급 수량", example = "100")
    private Integer totalQuantity;

    @Schema(description = "발급된 수량", example = "45")
    private Integer issuedQuantity;

    @Schema(description = "남은 수량", example = "55")
    private Integer remainingQuantity;

    @Schema(description = "유효 시작일", example = "2024-01-01T00:00:00")
    private LocalDateTime validFrom;

    @Schema(description = "유효 종료일", example = "2024-12-31T23:59:59")
    private LocalDateTime validUntil;
}