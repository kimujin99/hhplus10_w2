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
@Schema(description = "포인트 잔액 응답")
public class PointResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자명", example = "홍길동")
    private String userName;

    @Schema(description = "포인트 잔액", example = "50000")
    private Long pointBalance;

    @Schema(description = "갱신 일시", example = "2024-01-20T10:30:00")
    private LocalDateTime updatedAt;
}