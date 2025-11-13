package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.application.service.CouponService;
import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

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
    private static final int CONCURRENT_USERS = 20;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();

        // 제한된 수량의 쿠폰 생성
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

        // 동시성 테스트를 위한 사용자들 생성
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            userRepository.save(User.builder().build());
        }
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동시성 테스트: 20명이 동시에 쿠폰 발급 요청 시 정확히 10개만 발급")
    void issueCoupon_ConcurrentRequests_OnlyTotalQuantityIssued() throws InterruptedException {
        // given
        List<User> users = userRepository.findAll();
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // when: 20명의 사용자가 동시에 쿠폰 발급 요청
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(couponId);
                    couponService.issueCoupon(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue();

        // 디버깅: 결과 출력
        System.out.println("=== Test Results ===");
        System.out.println("Success count: " + successCount.get());
        System.out.println("Fail count: " + failCount.get());
        System.out.println("Exceptions count: " + exceptions.size());

        if (!exceptions.isEmpty()) {
            System.out.println("\n=== First Exception Details ===");
            Exception firstException = exceptions.get(0);
            System.out.println("Type: " + firstException.getClass().getName());
            System.out.println("Message: " + firstException.getMessage());
            firstException.printStackTrace();
        }

        // 성공한 발급 수는 쿠폰 총 수량과 같아야 함
        assertThat(successCount.get()).isEqualTo(TOTAL_COUPON_QUANTITY);

        // 실패한 발급 수는 나머지 요청 수와 같아야 함
        assertThat(failCount.get()).isEqualTo(CONCURRENT_USERS - TOTAL_COUPON_QUANTITY);

        // DB에 저장된 UserCoupon 수 확인
        long issuedCouponCount = userCouponRepository.count();
        assertThat(issuedCouponCount).isEqualTo(TOTAL_COUPON_QUANTITY);

        // Coupon의 issuedQuantity 확인
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(TOTAL_COUPON_QUANTITY);
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 동시에 쿠폰 발급 요청 시 정확히 10개만 발급")
    void issueCoupon_HighConcurrency_OnlyTotalQuantityIssued() throws InterruptedException {
        // given
        int highConcurrentUsers = 100;

        // 추가 사용자 생성
        List<User> existingUsers = userRepository.findAll();
        for (int i = existingUsers.size(); i < highConcurrentUsers; i++) {
            userRepository.save(User.builder().build());
        }

        List<User> users = userRepository.findAll();
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(highConcurrentUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명의 사용자가 동시에 쿠폰 발급 요청
        for (int i = 0; i < highConcurrentUsers; i++) {
            final Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(couponId);
                    couponService.issueCoupon(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(TOTAL_COUPON_QUANTITY);
        assertThat(failCount.get()).isEqualTo(highConcurrentUsers - TOTAL_COUPON_QUANTITY);

        // DB 검증
        long issuedCouponCount = userCouponRepository.count();
        assertThat(issuedCouponCount).isEqualTo(TOTAL_COUPON_QUANTITY);

        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(TOTAL_COUPON_QUANTITY);
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트: 재시도 로직이 정상 동작하여 낙관적 락 충돌 해결")
    void issueCoupon_OptimisticLockRetry_SuccessfullyResolvesConflicts() throws InterruptedException {
        // given
        int concurrentUsers = 5;
        List<User> users = userRepository.findAll().subList(0, concurrentUsers);

        // 쿠폰을 5개 수량으로 새로 생성
        couponRepository.deleteAll();
        Coupon smallCoupon = couponRepository.save(Coupon.builder()
                .name("소량 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(5)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(7))
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // when: 5명의 사용자가 정확히 동시에 쿠폰 발급 요청
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < concurrentUsers; i++) {
            final Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    // 모든 스레드가 준비될 때까지 대기
                    startLatch.await();

                    IssueCouponRequest request = new IssueCouponRequest(smallCoupon.getId());
                    couponService.issueCoupon(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 동시에 시작하도록 신호
        Thread.sleep(100); // 스레드들이 대기 상태에 들어갈 시간
        startLatch.countDown();

        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue();

        // 모든 요청이 성공해야 함 (재시도 덕분에)
        assertThat(successCount.get()).isEqualTo(concurrentUsers);
        assertThat(exceptions).isEmpty();

        // DB 검증
        long issuedCouponCount = userCouponRepository.count();
        assertThat(issuedCouponCount).isEqualTo(concurrentUsers);

        Coupon updatedCoupon = couponRepository.findById(smallCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(concurrentUsers);
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트: 같은 사용자가 동일 쿠폰을 여러 번 발급 시도해도 1개만 발급")
    void issueCoupon_SameUserMultipleAttempts_OnlyOneIssued() throws InterruptedException {
        // given
        User user = userRepository.findAll().get(0);
        int attemptCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 같은 사용자가 10번 동시에 쿠폰 발급 요청
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(couponId);
                    couponService.issueCoupon(user.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue();

        // 1번만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);

        // 나머지는 중복 발급 오류로 실패
        assertThat(failCount.get()).isEqualTo(attemptCount - 1);

        // 해당 사용자의 쿠폰은 1개만 발급되어야 함
        long userCouponCount = userCouponRepository.findByUserId(user.getId()).size();
        assertThat(userCouponCount).isEqualTo(1);
    }
}
