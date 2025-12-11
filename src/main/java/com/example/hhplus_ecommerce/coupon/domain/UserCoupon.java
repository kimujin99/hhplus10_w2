package com.example.hhplus_ecommerce.coupon.domain;

import com.example.hhplus_ecommerce.common.model.BaseEntity;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    @ColumnDefault("ISSUED") @Builder.Default
    private UserCouponStatus status = UserCouponStatus.ISSUED;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Coupon coupon;

    @Version
    private Long version;

    public void use() {
        if(isUsed()) {
            throw new ConflictException(CouponErrorCode.COUPON_ALREADY_USED);
        }
        this.status = UserCouponStatus.USED;
    }

    public void cancelUse() {
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