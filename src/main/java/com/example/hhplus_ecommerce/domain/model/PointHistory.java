package com.example.hhplus_ecommerce.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class PointHistory extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    private Long orderId;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    private Long amount;
    private Long balanceAfter;

    public enum TransactionType {
        CHARGE,
        USE
    }
}