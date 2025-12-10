package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.coupon.application.CouponService;
import com.example.hhplus_ecommerce.coupon.domain.Coupon;
import com.example.hhplus_ecommerce.user.domain.User;
import com.example.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.coupon.infrastructure.scheduler.CouponIssueScheduler;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import com.example.hhplus_ecommerce.coupon.presentaion.dto.CouponDto.*;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 쿠폰 발급 동시성 테스트
 * <p>
 * 새로운 Redis 기반 비동기 발급 시스템 테스트:
 * 1. Redis Set을 통한 중복 발급 방지
 * 2. Redis List 큐를 통한 비동기 DB 저장
 * 3. 스케줄러를 통한 큐 처리
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

    @Autowired
    private CouponIssueScheduler couponIssueScheduler;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long couponId;
    private static final int TOTAL_COUPON_QUANTITY = 10;
    private static final int CONCURRENT_USERS = 100;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화
        Set<String> keys = redisTemplate.keys("coupon:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 쿠폰 생성
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

        // Redis 캐시 초기화 (stock 설정)
        couponService.initializeCouponCache(couponId);

        // 사용자 생성
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            userRepository.save(User.builder().build());
        }
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 동시에 쿠폰 발급 요청 시 정확히 10명만 성공")
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

        // then: Redis 상태 검증 (큐 처리 전)
        String userSetKey = "coupon:user:" + couponId;
        Long setSize = redisTemplate.opsForSet().size(userSetKey);

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수 (큐에 추가된 수)").isEqualTo(TOTAL_COUPON_QUANTITY),
                () -> assertThat(soldOutCount.get()).as("재고 소진 에러").isEqualTo(CONCURRENT_USERS - TOTAL_COUPON_QUANTITY),
                () -> assertThat(unexpectedErrorCount.get()).as("예상치 못한 에러").isEqualTo(0),
                () -> assertThat(setSize).as("Redis Set에 저장된 사용자 수").isEqualTo(TOTAL_COUPON_QUANTITY)
        );

        // 스케줄러 수동 실행하여 큐 처리
        couponIssueScheduler.processQueue();

        // DB 상태 검증 (큐 처리 후)
        long issuedCouponCount = userCouponRepository.count();
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        List<UserCoupon> issuedCoupons = userCouponRepository.findAll();

        assertAll(
                () -> assertThat(issuedCouponCount).as("DB에 저장된 쿠폰 수").isEqualTo(TOTAL_COUPON_QUANTITY),
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

        // then: Redis 상태 검증
        String userSetKey = "coupon:user:" + couponId;
        Boolean isMember = redisTemplate.opsForSet().isMember(userSetKey, user.getId().toString());

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(1),
                () -> assertThat(alreadyIssuedCount.get()).as("중복 발급 에러").isEqualTo(attemptCount - 1),
                () -> assertThat(unexpectedErrorCount.get()).as("예상치 못한 에러").isEqualTo(0),
                () -> assertThat(isMember).as("Redis Set에 사용자가 있어야 함").isTrue()
        );

        // 스케줄러 수동 실행하여 큐 처리
        couponIssueScheduler.processQueue();

        // DB 상태 검증
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(user.getId());
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();

        assertAll(
                () -> assertThat(userCoupons).as("사용자가 받은 쿠폰 수")
                        .hasSize(1)
                        .allMatch(uc -> uc.getCoupon().getId().equals(couponId)),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).as("쿠폰 전체 발급 수량").isEqualTo(1)
        );
    }

    @Test
    @DisplayName("Redis Set 기반 중복 발급 방지 검증")
    void issueCoupon_RedisSetPrevents_Duplicates() {
        // given
        User user = userRepository.findAll().getFirst();
        IssueCouponRequest request = new IssueCouponRequest(couponId);

        // when: 첫 번째 발급
        couponService.issueCoupon(user.getId(), request);

        // then: 두 번째 발급 시도 시 예외
        assertThatThrownBy(() -> couponService.issueCoupon(user.getId(), request))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> {
                    ConflictException ce = (ConflictException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_ALREADY_ISSUED);
                });

        // Redis Set 검증
        String userSetKey = "coupon:user:" + couponId;
        Long setSize = redisTemplate.opsForSet().size(userSetKey);
        assertThat(setSize).isEqualTo(1);
    }

    @Test
    @DisplayName("재고 소진 시 Redis Set에서 롤백 검증")
    void issueCoupon_SoldOut_RollbackFromSet() throws InterruptedException {
        // given: 재고를 모두 소진
        List<User> users = userRepository.findAll();
        for (int i = 0; i < TOTAL_COUPON_QUANTITY; i++) {
            IssueCouponRequest request = new IssueCouponRequest(couponId);
            couponService.issueCoupon(users.get(i).getId(), request);
        }

        // when: 재고 소진 후 추가 발급 시도
        User extraUser = users.get(TOTAL_COUPON_QUANTITY);
        IssueCouponRequest request = new IssueCouponRequest(couponId);

        assertThatThrownBy(() -> couponService.issueCoupon(extraUser.getId(), request))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> {
                    ConflictException ce = (ConflictException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_SOLD_OUT);
                });

        // then: 롤백되어 Set에 추가 사용자가 없어야 함
        String userSetKey = "coupon:user:" + couponId;
        Long setSize = redisTemplate.opsForSet().size(userSetKey);
        Boolean isMember = redisTemplate.opsForSet().isMember(userSetKey, extraUser.getId().toString());

        assertAll(
                () -> assertThat(setSize).as("Set 크기는 재고만큼만").isEqualTo(TOTAL_COUPON_QUANTITY),
                () -> assertThat(isMember).as("실패한 사용자는 Set에 없어야 함").isFalse()
        );
    }

    @Test
    @DisplayName("큐 처리 테스트: 스케줄러가 큐의 데이터를 DB에 정확히 저장")
    void processQueue_SavesToDB_Correctly() {
        // given: 여러 사용자가 쿠폰 발급 요청
        List<User> users = userRepository.findAll();
        int issueCount = 5;

        for (int i = 0; i < issueCount; i++) {
            IssueCouponRequest request = new IssueCouponRequest(couponId);
            couponService.issueCoupon(users.get(i).getId(), request);
        }

        // 큐에 데이터가 있는지 확인
        String queueKey = "coupon:issue:queue:" + couponId;
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        assertThat(queueSize).as("큐에 발급 요청이 있어야 함").isEqualTo(issueCount);

        // when: 스케줄러 실행
        couponIssueScheduler.processQueue();

        // then: DB 검증
        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        Long remainingQueueSize = redisTemplate.opsForList().size(queueKey);

        assertAll(
                () -> assertThat(userCoupons).as("DB에 저장된 쿠폰 수").hasSize(issueCount),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).as("발급 수량 업데이트").isEqualTo(issueCount),
                () -> assertThat(remainingQueueSize).as("큐가 비어있어야 함").isEqualTo(0)
        );
    }
}