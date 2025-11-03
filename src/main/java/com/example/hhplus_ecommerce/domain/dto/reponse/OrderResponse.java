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
@Schema(description = "주문 응답 (목록용)")
public class OrderResponse {
    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "총 금액", example = "59800")
    private Long totalAmount;

    @Schema(description = "할인 금액", example = "5000")
    private Long discountAmount;

    @Schema(description = "최종 금액", example = "54800")
    private Long finalAmount;

    @Schema(description = "주문 상태 (PENDING, CONFIRMED, FAILED, CANCELLED)", example = "CONFIRMED")
    private String status;

    @Schema(description = "주문 일시", example = "2024-01-20T16:30:00")
    private LocalDateTime orderedAt;

    @Schema(description = "갱신 일시", example = "2024-01-20T16:35:00")
    private LocalDateTime updatedAt;
}