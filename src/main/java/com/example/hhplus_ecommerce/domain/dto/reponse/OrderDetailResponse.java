package com.example.hhplus_ecommerce.domain.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주문 상세 응답")
public class OrderDetailResponse {
    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "총 금액", example = "59800")
    private Long totalAmount;

    @Schema(description = "할인 금액", example = "5000")
    private Long discountAmount;

    @Schema(description = "최종 금액", example = "54800")
    private Long finalAmount;

    @Schema(description = "주문 상태 (PENDING, CONFIRMED, FAILED, CANCELLED)", example = "CONFIRMED")
    private String status;

    @Schema(description = "주문자명", example = "홍길동")
    private String ordererName;

    @Schema(description = "배송지 주소", example = "서울시 강남구 테헤란로 123")
    private String deliveryAddress;

    @Schema(description = "주문 일시", example = "2024-01-20T16:30:00")
    private LocalDateTime orderedAt;

    @Schema(description = "갱신 일시", example = "2024-01-20T16:35:00")
    private LocalDateTime updatedAt;

    @Schema(description = "사용자 쿠폰 ID", example = "1", nullable = true)
    private Long userCouponId;

    @Schema(description = "쿠폰명", example = "신규 가입 쿠폰", nullable = true)
    private String couponName;

    @Schema(description = "할인 유형", example = "FIXED", nullable = true)
    private String discountType;

    @Schema(description = "할인 값", example = "5000", nullable = true)
    private Long discountValue;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemResponse> items;
}