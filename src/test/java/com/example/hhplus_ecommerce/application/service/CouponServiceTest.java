package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 목록 조회 성공")
    void getCoupons_Success() {
        // given
        Coupon coupon1 = Coupon.builder()
                .name("쿠폰1")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        Coupon coupon2 = Coupon.builder()
                .name("쿠폰2")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(50)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();

        given(couponRepository.findAll()).willReturn(List.of(coupon1, coupon2));

        // when
        List<CouponResponse> result = couponService.getCoupons();

        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).name()).isEqualTo("쿠폰1"),
                () -> assertThat(result.get(1).name()).isEqualTo("쿠폰2")
        );

        verify(couponRepository).findAll();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        User user = User.builder().point(0L).build();
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        IssueCouponRequest request = new IssueCouponRequest(couponId);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(couponRepository.findByIdWithLockOrThrow(couponId)).willReturn(coupon);
        given(userCouponRepository.findByUserIdAndCouponIdWithLock(userId, couponId)).willReturn(Optional.empty());
        given(couponRepository.save(any(Coupon.class))).willReturn(coupon);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

        // when
        UserCouponResponse result = couponService.issueCoupon(userId, request);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findByIdOrThrow(userId);
        verify(couponRepository).findByIdWithLockOrThrow(couponId);
        verify(userCouponRepository).findByUserIdAndCouponIdWithLock(userId, couponId);
        verify(couponRepository).save(coupon);
        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 사용자 없음")
    void issueCoupon_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        IssueCouponRequest request = new IssueCouponRequest(1L);
        given(userRepository.findByIdOrThrow(userId)).willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(couponRepository, never()).findByIdWithLockOrThrow(any());
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 없음")
    void issueCoupon_Fail_CouponNotFound() {
        // given
        Long userId = 1L;
        Long couponId = 999L;
        User user = User.builder().point(0L).build();
        IssueCouponRequest request = new IssueCouponRequest(couponId);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(couponRepository.findByIdWithLockOrThrow(couponId)).willThrow(new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(couponRepository).findByIdWithLockOrThrow(couponId);
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급된 쿠폰")
    void issueCoupon_Fail_AlreadyIssued() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        User user = User.builder().point(0L).build();
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        UserCoupon existingUserCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        IssueCouponRequest request = new IssueCouponRequest(couponId);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(couponRepository.findByIdWithLockOrThrow(couponId)).willReturn(coupon);
        given(userCouponRepository.findByUserIdAndCouponIdWithLock(userId, couponId)).willReturn(Optional.of(existingUserCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_ISSUED);
        verify(userRepository).findByIdOrThrow(userId);
        verify(couponRepository).findByIdWithLockOrThrow(couponId);
        verify(userCouponRepository).findByUserIdAndCouponIdWithLock(userId, couponId);
        verify(couponRepository, never()).save(any());
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 성공")
    void getUserCoupons_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        User user = User.builder().point(0L).build();
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(userId)
                .coupon(coupon)
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(userCouponRepository.findByUserId(userId)).willReturn(List.of(userCoupon));

        // when
        List<UserCouponResponse> result = couponService.getUserCoupons(userId);

        // then
        assertThat(result).hasSize(1);
        verify(userRepository).findByIdOrThrow(userId);
        verify(userCouponRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 실패 - 사용자 없음")
    void getUserCoupons_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findByIdOrThrow(userId)).willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> couponService.getUserCoupons(userId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(userCouponRepository, never()).findByUserId(any());
    }
}