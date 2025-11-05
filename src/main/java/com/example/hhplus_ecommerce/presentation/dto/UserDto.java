package com.example.hhplus_ecommerce.presentation.dto;

import com.example.hhplus_ecommerce.domain.model.PointHistory;
import com.example.hhplus_ecommerce.domain.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class UserDto {

    public record PointResponse(
            Long userId,
            Long point
    ) {
        public static PointResponse from(User user) {
            return new PointResponse(
                    user.getId(),
                    user.getPoint()
            );
        }
    }

    public record PointHistoryResponse(
            Long pointHistoryId,
            Long userId,
            Long orderId,
            String transactionType,
            Long amount,
            Long balanceAfter,
            LocalDateTime createdAt
    ) {
        public static PointHistoryResponse from(PointHistory pointHistory) {
            return new PointHistoryResponse(
                    pointHistory.getId(),
                    pointHistory.getUserId(),
                    pointHistory.getOrderId(),
                    pointHistory.getTransactionType().name(),
                    pointHistory.getAmount(),
                    pointHistory.getBalanceAfter(),
                    pointHistory.getCreatedAt()
            );
        }

        public static List<PointHistoryResponse> fromList(List<PointHistory> pointHistories) {
            return pointHistories.stream()
                    .map(PointHistoryResponse::from)
                    .toList();
        }
    }

    public record ChargePointRequest(
            @NotNull(message = "충전 금액은 필수입니다")
            Long amount
    ) {
    }
}