package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class UserCoupon extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long couponId;
    private Long orderId;
    @Enumerated(EnumType.STRING)
    @ColumnDefault("ISSUED")
    @Builder.Default
    private UserCouponStatus status = UserCouponStatus.ISSUED;

    public void use() {
        if(isUsed()) {
            throw new ConflictException(CouponErrorCode.COUPON_ALREADY_USED);
        }
        this.status = UserCouponStatus.USED;
    }

    public void assignOrderId(Long orderId) {
        if(!isUsed()) {
            throw new ConflictException(CouponErrorCode.COUPON_NOT_USED);
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