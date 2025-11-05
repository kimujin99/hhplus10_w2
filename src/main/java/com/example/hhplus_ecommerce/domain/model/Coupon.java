package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Coupon extends BaseEntity {
    private String name;
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
            throw new BusinessException(ErrorCode.COUPON_EXPIRED, "쿠폰 발급 기간이 종료되었습니다.");
        }
        if(getRemainingQuantity() <= 0) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT, "쿠폰이 모두 소진되었습니다.");
        }
        this.issuedQuantity++;
        onUpdate();
    }

    public Long calculateDiscountAmount(Long originalAmount) {
        if(this.discountType == DiscountType.PERCENTAGE) {
            return originalAmount * this.discountValue / 100;
        } else {
            return Math.min(this.discountValue, originalAmount);
        }
    }

    public Long applyDiscount(Long originalAmount) {
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(this.validFrom) || now.isAfter(this.validUntil)) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED, "쿠폰이 만료되었습니다.");
        }
        return originalAmount - calculateDiscountAmount(originalAmount);
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED
    }
}
