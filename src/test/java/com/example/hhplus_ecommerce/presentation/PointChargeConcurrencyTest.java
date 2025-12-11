package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.application.service.UserPointService;
import com.example.hhplus_ecommerce.application.service.UserService;
import com.example.hhplus_ecommerce.domain.model.PointHistory;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.PointHistoryRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.ChargePointRequest;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 포인트 충전 동시성 테스트
 */
public class PointChargeConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private UserPointService userPointService;

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        pointHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동시성 테스트: 한 명의 사용자가 1000원씩 동시에 100번 충전하면 잔여 포인트가 정확히 100000원")
    void chargePoint_HighConcurrency_CorrectTotalAmount() throws InterruptedException {
        // given
        User user = userRepository.save(User.builder().build());
        Long userId = user.getId();
        int chargeCount = 100;
        long chargeAmount = 1000L;
        long expectedPoint = chargeAmount * chargeCount;

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(chargeCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 같은 사용자가 1000원씩 100번 동시 충전
        for (int i = 0; i < chargeCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargePointRequest request = new ChargePointRequest((long) chargeAmount);
                    userPointService.chargePoint(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        User updatedUser = userRepository.findById(userId).orElseThrow();
        List<PointHistory> pointHistories = pointHistoryRepository.findByUserId(userId);
        long totalChargedFromHistory = pointHistories.stream()
                .filter(ph -> ph.getTransactionType() == PointHistory.TransactionType.CHARGE)
                .mapToLong(PointHistory::getAmount)
                .sum();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(chargeCount),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(0),
                () -> assertThat(updatedUser.getPoint()).as("사용자 최종 포인트").isEqualTo(expectedPoint),
                () -> assertThat(pointHistories).as("포인트 히스토리 개수").hasSize(chargeCount),
                () -> assertThat(totalChargedFromHistory).as("히스토리에 기록된 총 충전 금액").isEqualTo(expectedPoint)
        );
    }

    @Test
    @DisplayName("동시성 테스트: 여러 사용자가 각각 동시에 포인트 충전 시 각자의 포인트가 정확히 계산됨")
    void chargePoint_MultipleUsers_EachCorrectAmount() throws InterruptedException {
        // given
        int userCount = 10;
        int chargesPerUser = 10;
        long chargeAmount = 5000L;
        long expectedPointPerUser = chargeAmount * chargesPerUser;

        // 10명의 사용자 생성
        for (int i = 0; i < userCount; i++) {
            userRepository.save(User.builder().build());
        }
        List<User> users = userRepository.findAll();

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(userCount * chargesPerUser);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10명의 사용자가 각각 5000원씩 10번 충전
        for (User user : users) {
            for (int i = 0; i < chargesPerUser; i++) {
                executorService.submit(() -> {
                    try {
                        ChargePointRequest request = new ChargePointRequest(chargeAmount);
                        userPointService.chargePoint(user.getId(), request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        List<User> updatedUsers = userRepository.findAll();
        long totalPointHistories = pointHistoryRepository.count();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(userCount * chargesPerUser),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(0),
                () -> assertThat(updatedUsers).as("모든 사용자의 포인트가 정확해야 함")
                        .allMatch(user -> user.getPoint().equals(expectedPointPerUser)),
                () -> assertThat(totalPointHistories).as("포인트 히스토리 총 개수")
                        .isEqualTo(userCount * chargesPerUser)
        );
    }
}
