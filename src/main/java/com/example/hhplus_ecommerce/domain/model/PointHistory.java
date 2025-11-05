package com.example.hhplus_ecommerce.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointHistory extends BaseEntity {
    private Long userId;
    private Long orderId;
    private TransactionType transactionType;
    private Long amount;
    private Long balanceAfter;

    public enum TransactionType {
        CHARGE,
        USE
    }
}