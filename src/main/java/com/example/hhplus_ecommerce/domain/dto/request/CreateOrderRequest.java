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
@Schema(description = "주문 생성 요청")
public class CreateOrderRequest {
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "주문자명", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ordererName;

    @Schema(description = "배송지 주소", example = "서울시 강남구 테헤란로 123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deliveryAddress;

    @Schema(description = "사용자 쿠폰 ID", example = "1", nullable = true)
    private Long userCouponId;
}