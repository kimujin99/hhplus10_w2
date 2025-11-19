package com.example.hhplus_ecommerce.presentation.utils;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.infrastructure.repository.*;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class testDataGenerator {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    /**
     * 대량 테스트 데이터 생성
     */
    @Transactional
    public void createLargeTestData() {
        // 사용자 생성 (1000명)
        log.info("사용자 데이터 생성 중...");
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            users.add(User.builder()
                    .point(100000L)
                    .build());

            if (i % 100 == 0) {
                userRepository.saveAll(users);
                entityManager.flush();
                entityManager.clear();
                users.clear();
            }
        }
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
            entityManager.flush();
            entityManager.clear();
        }

        // 상품 생성 (500개)
        log.info("상품 데이터 생성 중...");
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            products.add(Product.builder()
                    .productName("Product " + i)
                    .description("Description " + i)
                    .price(10000L + (i * 100))
                    .originalStockQuantity(1000)
                    .stockQuantity(1000 - (i % 100))
                    .viewCount(i * 10)
                    .build());

            if (i % 100 == 0) {
                productRepository.saveAll(products);
                entityManager.flush();
                entityManager.clear();
                products.clear();
            }
        }
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
            entityManager.flush();
            entityManager.clear();
        }

        // 주문 생성 (5000건)
        log.info("주문 데이터 생성 중...");
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            orders.add(Order.builder()
                    .userId((long) ((i % 1000) + 1))
                    .totalAmount(50000L)
                    .discountAmount(0L)
                    .status(Order.OrderStatus.CONFIRMED)
                    .ordererName("User " + (i % 1000 + 1))
                    .deliveryAddress("Address " + i)
                    .build());

            if (i % 500 == 0) {
                orderRepository.saveAll(orders);
                entityManager.flush();
                entityManager.clear();
                orders.clear();
            }
        }
        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
            entityManager.flush();
            entityManager.clear();
        }

        // 주문 아이템 생성 (15000건 - 주문당 평균 3개)
        log.info("주문 아이템 데이터 생성 중...");
        List<OrderItem> orderItems = new ArrayList<>();
        for (long orderId = 1; orderId <= 5000; orderId++) {
            int itemCount = (int) (orderId % 5) + 1; // 1~5개
            for (int j = 0; j < itemCount; j++) {
                orderItems.add(OrderItem.builder()
                        .orderId(orderId)
                        .productId((long) ((orderId + j) % 500 + 1))
                        .productName("Product " + ((orderId + j) % 500 + 1))
                        .price(10000L)
                        .quantity(2)
                        .build());
            }

            if (orderId % 500 == 0) {
                orderItemRepository.saveAll(orderItems);
                entityManager.flush();
                entityManager.clear();
                orderItems.clear();
            }
        }
        if (!orderItems.isEmpty()) {
            orderItemRepository.saveAll(orderItems);
            entityManager.flush();
            entityManager.clear();
        }

        // 장바구니 아이템 생성 (3000건)
        log.info("장바구니 데이터 생성 중...");
        List<CartItem> cartItems = new ArrayList<>();
        for (int i = 1; i <= 3000; i++) {
            cartItems.add(CartItem.builder()
                    .userId((long) ((i % 1000) + 1))
                    .productId((long) ((i % 500) + 1))
                    .productName("Product " + ((i % 500) + 1))
                    .price(10000L)
                    .quantity(1)
                    .build());

            if (i % 500 == 0) {
                cartItemRepository.saveAll(cartItems);
                entityManager.flush();
                entityManager.clear();
                cartItems.clear();
            }
        }
        if (!cartItems.isEmpty()) {
            cartItemRepository.saveAll(cartItems);
            entityManager.flush();
            entityManager.clear();
        }

        log.info("테스트 데이터 생성 완료:");
        log.info("- Users: 1000건");
        log.info("- Products: 500건");
        log.info("- Orders: 5000건");
        log.info("- OrderItems: ~15000건");
        log.info("- CartItems: 3000건");
    }
}
