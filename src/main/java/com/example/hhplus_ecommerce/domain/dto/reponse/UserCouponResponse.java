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
@Schema(description = "사용자 쿠폰 응답")
public class UserCouponResponse {
    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private Long userCouponId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

    @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
    private String name;

    @Schema(description = "할인 유형 (FIXED, PERCENTAGE)", example = "FIXED")
    private String discountType;

    @Schema(description = "할인 값", example = "5000")
    private Long discountValue;

    @Schema(description = "유효 시작일", example = "2024-01-01T00:00:00")
    private LocalDateTime validFrom;

    @Schema(description = "유효 종료일", example = "2024-12-31T23:59:59")
    private LocalDateTime validUntil;

    @Schema(description = "쿠폰 상태 (ISSUED, USED)", example = "ISSUED")
    private String status;

    @Schema(description = "발급 일시", example = "2024-01-20T15:00:00")
    private LocalDateTime issuedAt;

    @Schema(description = "사용 일시", example = "2024-01-20T15:00:00", nullable = true)
    private LocalDateTime usedAt;
}