package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.order.application.MakePaymentUseCase;
import com.example.hhplus_ecommerce.coupon.domain.Coupon;
import com.example.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.order.domain.Order;
import com.example.hhplus_ecommerce.order.domain.OrderItem;
import com.example.hhplus_ecommerce.order.infrastructure.OrderItemRepository;
import com.example.hhplus_ecommerce.order.infrastructure.OrderRepository;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import com.example.hhplus_ecommerce.product.domain.Product;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 결제 처리 동시성 테스트
 *
 * 테스트하는 동시성 이슈:
 * 1. 재고 차감 (Lost Update)
 * 2. 포인트 사용 (Lost Update)
 * 3. 쿠폰 사용 (중복 사용)
 */
public class MakePaymentConccurencyTest extends AbstractIntegrationTest {

    @Autowired
    private MakePaymentUseCase makePaymentUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("동시성 테스트: 재고가 1개 남은 상품에 대해 동시에 2명이 결제 요청 시 1명만 성공")
    void execute_SingleStockTwoConcurrentPayments_OnlyOneSucceeds() throws InterruptedException {
        // given: 재고가 1개인 상품 생성
        User user1 = userRepository.save(User.builder().point(10000L).build());
        User user2 = userRepository.save(User.builder().point(10000L).build());

        Product product = productRepository.save(Product.builder()
                .productName("테스트 상품")
                .price(5000L)
                .originalStockQuantity(1)
                .stockQuantity(1)
                .build());

        // 2개의 주문 생성 (PENDING 상태)
        Order order1 = orderRepository.save(Order.builder()
                .userId(user1.getId())
                .totalAmount(5000L)
                .discountAmount(0L)
                .status(Order.OrderStatus.PENDING)
                .build());

        Order order2 = orderRepository.save(Order.builder()
                .userId(user2.getId())
                .totalAmount(5000L)
                .discountAmount(0L)
                .status(Order.OrderStatus.PENDING)
                .build());

        // 각 주문에 대한 OrderItem 생성
        orderItemRepository.save(OrderItem.builder()
                .orderId(order1.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(1)
                .price(product.getPrice())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .orderId(order2.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(1)
                .price(product.getPrice())
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 2명이 동시에 결제 요청
        executorService.submit(() -> {
            try {
                makePaymentUseCase.execute(order1.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                makePaymentUseCase.execute(order2.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 1명만 성공, 재고는 0
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(1),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(1),
                () -> assertThat(updatedProduct.getStockQuantity()).as("최종 재고").isEqualTo(0)
        );
    }

    @Test
    @DisplayName("동시성 테스트: 동일 사용자가 포인트 10000원으로 10000원짜리 상품을 동시에 2번 결제 시 1번만 성공")
    void execute_InsufficientPointsTwoConcurrentPayments_OnlyOneSucceeds() throws InterruptedException {
        // given: 포인트 10000원을 가진 사용자
        User user = userRepository.save(User.builder().point(10000L).build());

        // 재고가 충분한 상품 (재고 이슈가 아닌 포인트 이슈 테스트)
        Product product = productRepository.save(Product.builder()
                .productName("테스트 상품")
                .price(10000L)
                .originalStockQuantity(10)
                .stockQuantity(10)
                .build());

        // 2개의 주문 생성
        Order order1 = orderRepository.save(Order.builder()
                .userId(user.getId())
                .totalAmount(10000L)
                .discountAmount(0L)
                .status(Order.OrderStatus.PENDING)
                .build());

        Order order2 = orderRepository.save(Order.builder()
                .userId(user.getId())
                .totalAmount(10000L)
                .discountAmount(0L)
                .status(Order.OrderStatus.PENDING)
                .build());

        orderItemRepository.save(OrderItem.builder()
                .orderId(order1.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(1)
                .price(product.getPrice())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .orderId(order2.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(1)
                .price(product.getPrice())
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 동시에 2번 결제 요청
        executorService.submit(() -> {
            try {
                makePaymentUseCase.execute(order1.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                makePaymentUseCase.execute(order2.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 1명만 성공, 포인트는 0
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(1),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(1),
                () -> assertThat(updatedUser.getPoint()).as("최종 포인트").isEqualTo(0L)
        );
    }

    @Test
    @DisplayName("동시성 테스트: 동일 쿠폰을 사용하여 동시에 2번 결제 시 1번만 성공")
    void execute_SameCouponTwoConcurrentPayments_OnlyOneSucceeds() throws InterruptedException {
        // given: 사용자와 쿠폰 생성
        User user = userRepository.save(User.builder().point(20000L).build());

        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("10% 할인 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(1)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(7))
                .build());

        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.builder()
                .userId(user.getId())
                .coupon(coupon)
                .status(UserCoupon.UserCouponStatus.ISSUED)
                .build());

        // 재고가 충분한 상품
        Product product = productRepository.save(Product.builder()
                .productName("테스트 상품")
                .price(10000L)
                .originalStockQuantity(10)
                .stockQuantity(10)
                .build());

        // 2개의 주문 생성 (둘 다 같은 쿠폰 사용)
        Order order1 = orderRepository.save(Order.builder()
                .userId(user.getId())
                .userCouponId(userCoupon.getId())
                .totalAmount(10000L)
                .discountAmount(1000L)
                .status(Order.OrderStatus.PENDING)
                .build());

        Order order2 = orderRepository.save(Order.builder()
                .userId(user.getId())
                .userCouponId(userCoupon.getId())
                .totalAmount(10000L)
                .discountAmount(1000L)
                .status(Order.OrderStatus.PENDING)
                .build());

        orderItemRepository.save(OrderItem.builder()
                .orderId(order1.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(1)
                .price(product.getPrice())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .orderId(order2.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(1)
                .price(product.getPrice())
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 같은 쿠폰으로 동시에 2번 결제 요청
        executorService.submit(() -> {
            try {
                makePaymentUseCase.execute(order1.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                makePaymentUseCase.execute(order2.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 1번만 성공, 쿠폰은 USED 상태
        UserCoupon updatedUserCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(1),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(1),
                () -> assertThat(updatedUserCoupon.getStatus()).as("쿠폰 상태").isEqualTo(UserCoupon.UserCouponStatus.USED)
        );
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 재고 100개인 상품을 동시에 1개씩 구매 시 모두 성공하고 재고는 0")
    void execute_HighConcurrency_AllSucceedWithExactStock() throws InterruptedException {
        // given: 재고 100개인 상품과 100명의 사용자
        int userCount = 100;
        int initialStock = 100;

        Product product = productRepository.save(Product.builder()
                .productName("인기 상품")
                .price(1000L)
                .originalStockQuantity(initialStock)
                .stockQuantity(initialStock)
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명이 동시에 결제
        for (int i = 0; i < userCount; i++) {
            executorService.submit(() -> {
                try {
                    // 각 스레드마다 사용자, 주문, 주문아이템 생성
                    User user = userRepository.save(User.builder().point(10000L).build());

                    Order order = orderRepository.save(Order.builder()
                            .userId(user.getId())
                            .totalAmount(1000L)
                            .discountAmount(0L)
                            .status(Order.OrderStatus.PENDING)
                            .build());

                    orderItemRepository.save(OrderItem.builder()
                            .orderId(order.getId())
                            .productId(product.getId())
                            .productName(product.getProductName())
                            .quantity(1)
                            .price(product.getPrice())
                            .build());

                    makePaymentUseCase.execute(order.getId());
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

        // then: 모두 성공, 재고는 0
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(userCount),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(0),
                () -> assertThat(updatedProduct.getStockQuantity()).as("최종 재고").isEqualTo(0),
                () -> assertThat(updatedProduct.getPurchaseCount()).as("구매 횟수").isEqualTo(userCount)
        );
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 재고 50개인 상품을 동시에 구매 시 50명만 성공")
    void execute_HighConcurrencyInsufficientStock_ExactlyHalfSucceed() throws InterruptedException {
        // given: 재고 50개인 상품
        int userCount = 100;
        int initialStock = 50;

        Product product = productRepository.save(Product.builder()
                .productName("한정판 상품")
                .price(1000L)
                .originalStockQuantity(initialStock)
                .stockQuantity(initialStock)
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명이 동시에 결제
        for (int i = 0; i < userCount; i++) {
            executorService.submit(() -> {
                try {
                    User user = userRepository.save(User.builder().point(10000L).build());

                    Order order = orderRepository.save(Order.builder()
                            .userId(user.getId())
                            .totalAmount(1000L)
                            .discountAmount(0L)
                            .status(Order.OrderStatus.PENDING)
                            .build());

                    orderItemRepository.save(OrderItem.builder()
                            .orderId(order.getId())
                            .productId(product.getId())
                            .productName(product.getProductName())
                            .quantity(1)
                            .price(product.getPrice())
                            .build());

                    makePaymentUseCase.execute(order.getId());
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

        // then: 50명만 성공, 재고는 0
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertAll(
                () -> assertThat(completed).as("테스트가 30초 내에 완료되어야 함").isTrue(),
                () -> assertThat(successCount.get()).as("성공 횟수").isEqualTo(initialStock),
                () -> assertThat(failCount.get()).as("실패 횟수").isEqualTo(userCount - initialStock),
                () -> assertThat(updatedProduct.getStockQuantity()).as("최종 재고").isEqualTo(0)
        );
    }
}