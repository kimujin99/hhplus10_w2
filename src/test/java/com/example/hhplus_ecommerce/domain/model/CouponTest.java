package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class CouponTest {

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issue_Success() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when
        coupon.issue();

        // then
        assertAll(
                () -> assertThat(coupon.getIssuedQuantity()).isEqualTo(1),
                () -> assertThat(coupon.getRemainingQuantity()).isEqualTo(99)
        );
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 수량 부족")
    void issue_Fail_SoldOut() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(1)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when & then
        assertThatThrownBy(coupon::issue)
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_SOLD_OUT);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 발급 기간 종료")
    void issue_Fail_Expired() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();

        // when & then
        assertThatThrownBy(coupon::issue)
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_EXPIRED);
    }

    @Test
    @DisplayName("할인 금액 계산 - 퍼센트 타입")
    void calculateDiscountAmount_Percentage() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when
        Long discountAmount = coupon.calculateDiscountAmount(10000L);

        // then
        assertThat(discountAmount).isEqualTo(1000L);
    }

    @Test
    @DisplayName("할인 금액 계산 - 고정 금액 타입")
    void calculateDiscountAmount_Fixed() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when
        Long discountAmount = coupon.calculateDiscountAmount(10000L);

        // then
        assertThat(discountAmount).isEqualTo(5000L);
    }

    @Test
    @DisplayName("할인 금액 계산 - 고정 금액이 원가보다 큰 경우")
    void calculateDiscountAmount_Fixed_ExceedsOriginal() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when
        Long discountAmount = coupon.calculateDiscountAmount(3000L);

        // then
        assertThat(discountAmount).isEqualTo(3000L);
    }

    @Test
    @DisplayName("쿠폰 만료 확인 - 사용 가능한 기간")
    void isExpired_Valid() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when
        boolean expired = coupon.isExpired();

        // then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("쿠폰 만료 확인 - 만료됨")
    void isExpired_Expired() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();

        // when
        boolean expired = coupon.isExpired();

        // then
        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("쿠폰 만료 확인 - 아직 시작 전")
    void isExpired_BeforeStart() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        // when
        boolean expired = coupon.isExpired();

        // then
        assertThat(expired).isTrue();
    }
}