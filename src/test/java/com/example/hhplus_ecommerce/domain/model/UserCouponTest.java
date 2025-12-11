package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.common.presentation.exception.BaseException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.coupon.domain.Coupon;
import com.example.hhplus_ecommerce.coupon.domain.UserCoupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserCouponTest {

    @Test
    @DisplayName("사용자 쿠폰 생성 시 상태는 ISSUED")
    void createUserCoupon_InitialStatusIsIssued() {
        // given & when
        Coupon coupon = Coupon.builder().id(1L).build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .coupon(coupon)
                .build();

        // then
        assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.ISSUED),
                () -> assertThat(userCoupon.isUsed()).isFalse()
        );
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void use_Success() {
        // given
        Coupon coupon = Coupon.builder().id(1L).build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .coupon(coupon)
                .build();

        // when
        userCoupon.use();

        // then
        assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.USED),
                () -> assertThat(userCoupon.isUsed()).isTrue()
        );
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 이미 사용된 쿠폰")
    void use_Fail_AlreadyUsed() {
        // given
        Coupon coupon = Coupon.builder().id(1L).build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .coupon(coupon)
                .build();
        userCoupon.use();

        // when & then
        assertThatThrownBy(userCoupon::use)
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_USED);
    }
}