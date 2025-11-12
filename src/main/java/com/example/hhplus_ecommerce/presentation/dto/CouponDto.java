package com.example.hhplus_ecommerce.presentation.dto;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class CouponDto {

    public record CouponResponse(
            Long couponId,
            String name,
            String discountType,
            Long discountValue,
            Integer totalQuantity,
            Integer issuedQuantity,
            Integer remainingQuantity,
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
        public static CouponResponse from(Coupon coupon) {
            return new CouponResponse(
                    coupon.getId(),
                    coupon.getName(),
                    coupon.getDiscountType().name(),
                    coupon.getDiscountValue(),
                    coupon.getTotalQuantity(),
                    coupon.getIssuedQuantity(),
                    coupon.getRemainingQuantity(),
                    coupon.getValidFrom(),
                    coupon.getValidUntil()
            );
        }

        public static List<CouponResponse> fromList(List<Coupon> coupons) {
            return coupons.stream()
                    .map(CouponResponse::from)
                    .toList();
        }
    }

    public record UserCouponResponse(
            Long userCouponId,
            Long userId,
            Long couponId,
            String couponName,
            String discountType,
            Long discountValue,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            String status,
            LocalDateTime issuedAt
    ) {
        public static UserCouponResponse from(UserCoupon userCoupon, Coupon coupon) {
            return new UserCouponResponse(
                    userCoupon.getId(),
                    userCoupon.getUserId(),
                    userCoupon.getCoupon().getId(),
                    coupon.getName(),
                    coupon.getDiscountType().name(),
                    coupon.getDiscountValue(),
                    coupon.getValidFrom(),
                    coupon.getValidUntil(),
                    userCoupon.getStatus().name(),
                    userCoupon.getCreatedAt()
            );
        }
    }

    public record IssueCouponRequest(
            @NotNull(message = "쿠폰 ID는 필수입니다")
            Long couponId
    ) {
    }
}