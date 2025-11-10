package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserCoupon extends BaseEntity {
    private Long userId;
    private Long couponId;
    private Long orderId;
    private UserCouponStatus status;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    @Builder
    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.orderId = null;
        this.status = UserCouponStatus.ISSUED;
    }

    public void use() {
        if(isUsed()) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
        }
        this.status = UserCouponStatus.USED;
    }

    public void assignOrderId(Long orderId) {
        if(!isUsed()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_USED);
        }
        this.orderId = orderId;
    }

    public void restore() {
        this.orderId = null;
        this.status = UserCouponStatus.ISSUED;
    }

    public boolean isUsed() {
        return this.status == UserCouponStatus.USED;
    }

    public enum UserCouponStatus {
        ISSUED,
        USED
    }
}