package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.application.service.CouponService;
import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 쿠폰 발급 동시성 테스트
 */
class CouponIssueConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Long couponId;
    private static final int TOTAL_COUPON_QUANTITY = 10;
    private static final int CONCURRENT_USERS = 100;

    @BeforeEach
    void setUp() {
        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("선착순 할인 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(TOTAL_COUPON_QUANTITY)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(7))
                .build());
        this.couponId = coupon.getId();

        // Redis 카운터 초기화
        couponService.initializeCouponCache(couponId);

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            userRepository.save(User.builder().build());
        }
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 동시에 쿠폰 발급 요청 시 정확히 10개만 발급")
    void issueCoupon_HighConcurrency_OnlyTotalQuantityIssued() throws InterruptedException {
        // given
        List<User> users = userRepository.findAll();
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger soldOutCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        // when: 100명의 사용자가 동시에 쿠폰 발급 요청
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(couponId);
                    couponService.issueCoupon(userId, request);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    if (e.getErrorCode() == CouponErrorCode.COUPON_SOLD_OUT) {
                        soldOutCount.incrementAndGet();
                    } else {
                        unexpectedErrorCount.incrementAndGet();
                        System.err.println("Unexpected ConflictException: " + e.getErrorCode() + " - " + e.getMessage());
                    }
                } catch (Exception e) {
                    unexpectedErrorCount.incrementAndGet();
                    System.err.println("Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        long issuedCouponCount = userCouponRepository.count();
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        List<UserCoupon> issuedCoupons = userCouponRepository.findAll();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(TOTAL_COUPON_QUANTITY),
                () -> assertThat(soldOutCount.get()).as("재고 소진 에러").isEqualTo(CONCURRENT_USERS - TOTAL_COUPON_QUANTITY),
                () -> assertThat(unexpectedErrorCount.get()).as("예상치 못한 에러").isEqualTo(0),
                () -> assertThat(issuedCouponCount).as("발급된 쿠폰 수").isEqualTo(TOTAL_COUPON_QUANTITY),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).as("쿠폰 발급 수량").isEqualTo(TOTAL_COUPON_QUANTITY),
                () -> assertThat(updatedCoupon.getRemainingQuantity()).as("쿠폰 남은 수량").isEqualTo(0),
                () -> assertThat(issuedCoupons)
                        .as("발급된 쿠폰들은 모두 다른 사용자여야 함")
                        .hasSize(TOTAL_COUPON_QUANTITY)
                        .extracting(UserCoupon::getUserId)
                        .doesNotHaveDuplicates()
        );
    }

    @Test
    @DisplayName("동시성 테스트: 같은 사용자가 동일 쿠폰을 여러 번 발급 시도해도 1개만 발급")
    void issueCoupon_SameUserMultipleAttempts_OnlyOneIssued() throws InterruptedException {
        // given
        User user = userRepository.findAll().getFirst();
        int attemptCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyIssuedCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        // when: 같은 사용자가 10번 동시에 쿠폰 발급 요청
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(couponId);
                    couponService.issueCoupon(user.getId(), request);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    if (e.getErrorCode() == CouponErrorCode.COUPON_ALREADY_ISSUED) {
                        alreadyIssuedCount.incrementAndGet();
                    } else {
                        unexpectedErrorCount.incrementAndGet();
                        System.err.println("Unexpected ConflictException: " + e.getErrorCode() + " - " + e.getMessage());
                    }
                } catch (Exception e) {
                    unexpectedErrorCount.incrementAndGet();
                    System.err.println("Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(user.getId());
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(1),
                () -> assertThat(alreadyIssuedCount.get()).as("중복 발급 에러").isEqualTo(attemptCount - 1),
                () -> assertThat(unexpectedErrorCount.get()).as("예상치 못한 에러").isEqualTo(0),
                () -> assertThat(userCoupons).as("사용자가 받은 쿠폰 수")
                        .hasSize(1)
                        .allMatch(uc -> uc.getCoupon().getId().equals(couponId)),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).as("쿠폰 전체 발급 수량").isEqualTo(1)
        );
    }
}
