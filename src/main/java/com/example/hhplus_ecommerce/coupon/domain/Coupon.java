package com.example.hhplus_ecommerce.coupon.domain;

import com.example.hhplus_ecommerce.common.model.BaseEntity;
import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CouponErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class Coupon extends BaseEntity {
    private String name;
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    private Long discountValue;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    public Integer getRemainingQuantity() {
        return this.totalQuantity - this.issuedQuantity;
    }

    public void issue() {
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(validUntil)) {
            throw new ConflictException(CouponErrorCode.COUPON_EXPIRED, "쿠폰 발급 기간이 종료되었습니다.");
        }
        if(getRemainingQuantity() <= 0) {
            throw new ConflictException(CouponErrorCode.COUPON_SOLD_OUT);
        }
        this.issuedQuantity++;
    }

    public Long calculateDiscountAmount(Long originalAmount) {
        if(this.discountType == DiscountType.PERCENTAGE) {
            return originalAmount * this.discountValue / 100;
        } else {
            return Math.min(this.discountValue, originalAmount);
        }
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(this.validFrom) || now.isAfter(this.validUntil);
    }

    /**
     * 발급 수량을 증가시킵니다. (배치 처리용)
     *
     * @param count 증가시킬 수량
     */
    public void incrementIssuedQuantity(int count) {
        this.issuedQuantity += count;
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED
    }
}
