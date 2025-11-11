package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserCouponTest {

    @Test
    @DisplayName("사용자 쿠폰 생성 시 상태는 ISSUED")
    void createUserCoupon_InitialStatusIsIssued() {
        // given & when
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
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
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
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
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
                .build();
        userCoupon.use();

        // when & then
        assertThatThrownBy(userCoupon::use)
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    @DisplayName("주문 ID 할당 성공")
    void assignOrderId_Success() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
                .build();
        userCoupon.use();

        // when
        userCoupon.assignOrderId(100L);

        // then
        assertThat(userCoupon.getOrderId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("주문 ID 할당 실패 - 사용되지 않은 쿠폰")
    void assignOrderId_Fail_NotUsed() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
                .build();

        // when & then
        assertThatThrownBy(() -> userCoupon.assignOrderId(100L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_USED);
    }

    @Test
    @DisplayName("쿠폰 복구 성공")
    void restore_Success() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
                .build();
        userCoupon.use();
        userCoupon.assignOrderId(100L);

        // when
        userCoupon.restore();

        // then
        assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.ISSUED),
                () -> assertThat(userCoupon.getOrderId()).isNull(),
                () -> assertThat(userCoupon.isUsed()).isFalse()
        );
    }

    @Test
    @DisplayName("쿠폰 사용 및 복구 시나리오")
    void useAndRestoreScenario() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(1L)
                .build();

        // 초기 상태 확인
        assertThat(userCoupon.isUsed()).isFalse();

        // 쿠폰 사용
        userCoupon.use();
        assertThat(userCoupon.isUsed()).isTrue();

        // 주문 ID 할당
        userCoupon.assignOrderId(100L);
        assertThat(userCoupon.getOrderId()).isEqualTo(100L);

        // 쿠폰 복구
        userCoupon.restore();
        assertAll(
                () -> assertThat(userCoupon.isUsed()).isFalse(),
                () -> assertThat(userCoupon.getOrderId()).isNull()
        );

        // 다시 사용 가능
        userCoupon.use();
        assertThat(userCoupon.isUsed()).isTrue();
    }
}