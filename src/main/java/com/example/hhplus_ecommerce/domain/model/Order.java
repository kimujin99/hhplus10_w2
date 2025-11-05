package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Order extends BaseEntity {
    private Long userId;
    private Long totalAmount;
    private Long discountAmount;
    private OrderStatus status;
    private String ordererName;
    private String deliveryAddress;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    @Builder
    public Order(Long userId, Long totalAmount, Long discountAmount, String ordererName, String deliveryAddress) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.status = OrderStatus.PENDING;
        this.ordererName = ordererName;
        this.deliveryAddress = deliveryAddress;
    }

    public void confirm() {
        if(this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "결제할 수 없는 주문 상태입니다.");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void fail() {
        if(this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "실패 처리할 수 없는 주문 상태입니다.");
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
