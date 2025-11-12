package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.application.service.CouponService;
import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
@Testcontainers
class CouponConcurrencyTest {

    @Container
    static MySQLContainer<?> container = new MySQLContainer<>("mysql:latest")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("한정 수량 쿠폰을 동시에 발급할 때 정확히 50명만 발급받아야 함")
    void concurrentCouponIssue_OnlyAvailableQuantityShouldSucceed() throws InterruptedException {
        // given
        Coupon coupon = Coupon.builder()
                .name("선착순 50명 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(50)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        couponRepository.save(coupon);
        IssueCouponRequest request = new IssueCouponRequest(coupon.getId());

        int threadCount = 100; // 100명이 발급 시도
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    User user = User.builder()
                            .point(0L)
                            .build();
                    userRepository.save(user);
                    couponService.issueCoupon(user.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Coupon result = couponRepository.findById(coupon.getId()).orElseThrow();

        log.info("=== 쿠폰 발급 동시성 테스트 결과 ===");
        log.info("총 수량: 50개");
        log.info("발급 시도: {}명", threadCount);
        log.info("성공 카운트: {}", successCount.get());
        log.info("실패 카운트: {}", failCount.get());
        log.info("실제 발급 수량: {}개", result.getIssuedQuantity());
        log.info("남은 수량: {}개", result.getRemainingQuantity());

        // 동시성 제어가 제대로 되었다면:
        // 1. 정확히 50명만 성공
        // 2. 50명은 실패
        // 3. 발급 수량은 50
        // 4. 남은 수량은 0
        // 5. 초과 발급 없음
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(result.getIssuedQuantity()).isEqualTo(50);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("쿠폰 소진 직전 동시 발급 시 정확히 1명만 성공해야 함")
    void concurrentCouponIssueNearLimit_OnlyOneLeftShouldSucceed() throws InterruptedException {
        // given
        Coupon coupon = Coupon.builder()
                .name("선착순 10명 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(10)
                .issuedQuantity(9)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        couponRepository.save(coupon);
        IssueCouponRequest request = new IssueCouponRequest(coupon.getId());

        int threadCount = 100; // 20명이 마지막 1개를 놓고 경쟁
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 마지막 쿠폰 발급 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    User user = User.builder()
                            .point(0L)
                            .build();
                    userRepository.save(user);
                    couponService.issueCoupon(user.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Coupon result = couponRepository.findById(coupon.getId()).orElseThrow();

        log.info("=== 쿠폰 소진 직전 동시성 테스트 결과 ===");
        log.info("총 수량: 10개");
        log.info("초기 발급: 9개");
        log.info("발급 시도: {}명", threadCount);
        log.info("성공 카운트: {}", successCount.get());
        log.info("실패 카운트: {}", failCount.get());
        log.info("최종 발급 수량: {}개", result.getIssuedQuantity());

        // 동시성 제어가 제대로 되었다면:
        // 1. 정확히 1명만 성공
        // 2. 99명은 실패
        // 3. 최종 발급 수량은 10
        // 4. 초과 발급 없음
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);
        assertThat(result.getIssuedQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("하나의 쿠폰을 한명이 동시에 발급할 때 정확히 1개만 발급받아야 함")
    void sameUserConcurrentCouponIssue_OnlyOneShouldSucceed() throws InterruptedException {
        // given
        Coupon coupon = Coupon.builder()
                .name("중복 발급 방지 테스트 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(3000L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        couponRepository.save(coupon);
        IssueCouponRequest request = new IssueCouponRequest(coupon.getId());

        // 한 명의 사용자 생성
        User user = User.builder()
                .point(0L)
                .build();
        userRepository.save(user);

        int threadCount = 50; // 동일 사용자가 50번 발급 시도
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 동시에 여러 번 발급 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(user.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Coupon result = couponRepository.findById(coupon.getId()).orElseThrow();

        log.info("=== 동일 사용자 중복 발급 방지 테스트 결과 ===");
        log.info("총 수량: 100개");
        log.info("발급 시도: {}회 (동일 사용자)", threadCount);
        log.info("성공 카운트: {}", successCount.get());
        log.info("실패 카운트: {}", failCount.get());
        log.info("실제 발급 수량: {}개", result.getIssuedQuantity());

        // 동시성 제어가 제대로 되었다면:
        // 1. 정확히 1번만 성공
        // 2. 나머지 49번은 중복 발급으로 실패
        // 3. 쿠폰 발급 수량은 1개만 증가
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(49);
        assertThat(result.getIssuedQuantity()).isEqualTo(1);
    }
}