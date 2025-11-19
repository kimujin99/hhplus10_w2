package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.OrderErrorCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "order_table")
public class Order extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = true)
    private Long userCouponId;
    private Long totalAmount;
    private Long discountAmount;
    @Enumerated(EnumType.STRING)
    @ColumnDefault("PENDING")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    private String ordererName;
    private String deliveryAddress;

    public void confirm() {
        if(this.status != OrderStatus.PENDING) {
            throw new ConflictException(OrderErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void fail() {
        if(this.status != OrderStatus.PENDING) {
            throw new ConflictException(OrderErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.FAILED;
    }

    public Long getFinalAmount() {
        return this.totalAmount - this.discountAmount;
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        FAILED
    }
}
