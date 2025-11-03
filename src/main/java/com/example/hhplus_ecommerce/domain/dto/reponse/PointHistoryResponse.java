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
@Schema(description = "포인트 거래 내역 응답")
public class PointHistoryResponse {
    @Schema(description = "포인트 이력 ID", example = "1")
    private Long pointHistoryId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "주문 ID", example = "5", nullable = true)
    private Long orderId;

    @Schema(description = "거래 유형 (CHARGE, USE, REFUND)", example = "USE")
    private String transactionType;

    @Schema(description = "거래 금액", example = "29900")
    private Long amount;

    @Schema(description = "거래 후 잔액", example = "120100")
    private Long balanceAfter;

    @Schema(description = "생성 일시", example = "2024-01-20T16:40:00")
    private LocalDateTime createdAt;
}