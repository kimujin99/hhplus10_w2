package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.common.presentation.exception.BaseException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.PointErrorCode;
import com.example.hhplus_ecommerce.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserTest {

    @Test
    @DisplayName("포인트 충전 성공 - 1000원 단위")
    void chargePoint_Success() {
        // given
        User user = User.builder().point(0L).build();
        Long chargeAmount = 5000L;

        // when
        user.chargePoint(chargeAmount);

        // then
        assertThat(user.getPoint()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("포인트 충전 실패 - 0원 이하")
    void chargePoint_Fail_ZeroOrNegative() {
        // given
        User user = User.builder().point(0L).build();

        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> user.chargePoint(0L))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CHARGE_AMOUNT),
                () -> assertThatThrownBy(() -> user.chargePoint(-1000L))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CHARGE_AMOUNT)
        );
    }

    @Test
    @DisplayName("포인트 충전 실패 - 1000원 단위가 아님")
    void chargePoint_Fail_NotMultipleOf1000() {
        // given
        User user = User.builder().point(0L).build();

        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> user.chargePoint(500L))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CHARGE_AMOUNT),
                () -> assertThatThrownBy(() -> user.chargePoint(1500L))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CHARGE_AMOUNT)
        );
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_Success() {
        // given
        User user = User.builder().point(0L).build();
        user.chargePoint(10000L);

        // when
        user.usePoint(3000L);

        // then
        assertThat(user.getPoint()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("포인트 사용 실패 - 잔액 부족")
    void usePoint_Fail_InsufficientPoint() {
        // given
        User user = User.builder().point(0L).build();
        user.chargePoint(5000L);

        // when & then
        assertThatThrownBy(() -> user.usePoint(6000L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INSUFFICIENT_POINT);
    }

    @Test
    @DisplayName("포인트 사용 실패 - 0원 이하")
    void usePoint_Fail_ZeroOrNegative() {
        // given
        User user = User.builder().point(0L).build();
        user.chargePoint(5000L);

        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> user.usePoint(0L))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_POINT_AMOUNT),
                () -> assertThatThrownBy(() -> user.usePoint(-1000L))
                        .isInstanceOf(BaseException.class)
                        .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_POINT_AMOUNT)
        );
    }

    @Test
    @DisplayName("포인트 충전 및 사용 복합 시나리오")
    void complexScenario() {
        // given
        User user = User.builder().point(0L).build();

        // when & then
        user.chargePoint(10000L);
        assertThat(user.getPoint()).isEqualTo(10000L);

        user.usePoint(3000L);
        assertThat(user.getPoint()).isEqualTo(7000L);

        user.chargePoint(5000L);
        assertThat(user.getPoint()).isEqualTo(12000L);

        user.usePoint(12000L);
        assertThat(user.getPoint()).isEqualTo(0L);
    }
}