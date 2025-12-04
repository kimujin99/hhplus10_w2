package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.application.service.PopularProductCacheService;
import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.CartDto.AddCartItemRequest;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.OrderRequest;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.ChargePointRequest;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인기 상품 캐시 비동기 업데이트 통합 테스트
 * <p>
 * 상품 조회 및 결제 완료 시 Redis 캐시에 점수가 비동기로 반영되는지 검증합니다.
 */
class PopularProductCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PopularProductCacheService cacheService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String POPULAR_PRODUCTS_KEY = "popular:products";

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화 (각 테스트마다 깨끗한 상태로 시작)
        cacheService.clearCache();
    }

    @Test
    @DisplayName("상품 조회 시 Redis 캐시에 조회수 점수가 비동기로 +1 반영된다")
    void productView_IncreasesScore_Async() throws Exception {
        // given: 상품 생성 및 초기 캐시 설정
        Product product = productRepository.save(Product.builder()
                .productName("테스트 상품")
                .price(10000L)
                .viewCount(0)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());

        // Redis 캐시 초기화 (초기 점수: 0)
        cacheService.initializeCache();

        Double initialScore = redisTemplate.opsForZSet()
                .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
        assertThat(initialScore).isEqualTo(0.0);

        // when: 상품 조회 API 호출 (비동기로 점수 +1)
        mockMvc.perform(get("/api/v1/products/{productId}", product.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        // then: 비동기 작업 완료 대기 후 점수 확인
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Double updatedScore = redisTemplate.opsForZSet()
                            .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
                    assertThat(updatedScore).isEqualTo(1.0);  // 0 + 1 = 1
                });
    }

    @Test
    @DisplayName("상품을 여러 번 조회하면 Redis 캐시 점수가 누적된다")
    void productView_MultipleViews_AccumulateScore() throws Exception {
        // given: 상품 생성 및 캐시 초기화
        Product product = productRepository.save(Product.builder()
                .productName("인기 상품")
                .price(20000L)
                .viewCount(0)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());

        cacheService.initializeCache();

        // when: 상품을 3번 조회
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/products/{productId}", product.getId()))
                    .andExpect(status().isOk());
        }

        // then: 점수가 3 증가
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Double score = redisTemplate.opsForZSet()
                            .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
                    assertThat(score).isEqualTo(3.0);  // 0 + 1 + 1 + 1 = 3
                });
    }

    @Test
    @DisplayName("결제 완료 시 Redis 캐시에 구매 점수가 비동기로 반영된다")
    void payment_IncreasesScore_Async() throws Exception {
        // given: 사용자 생성 및 포인트 충전
        User user = userRepository.save(User.builder().build());
        Long userId = user.getId();

        ChargePointRequest chargeRequest = new ChargePointRequest(1000000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andExpect(status().isOk());

        // 상품 생성 (수량 2개 구매 예정)
        Product product = productRepository.save(Product.builder()
                .productName("구매 상품")
                .price(10000L)
                .viewCount(0)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());

        // Redis 캐시 초기화
        cacheService.initializeCache();

        Double initialScore = redisTemplate.opsForZSet()
                .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
        assertThat(initialScore).isEqualTo(0.0);

        // 장바구니에 상품 추가 (수량 2)
        AddCartItemRequest cartRequest = new AddCartItemRequest(product.getId(), 2);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // 주문 생성
        OrderRequest orderRequest = new OrderRequest(userId, "테스터", "서울시", null);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .get("orderId")
                .asLong();

        // when: 결제 처리 (비동기로 점수 +수량*2)
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isOk());

        // then: 비동기 작업 완료 대기 후 점수 확인
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Double updatedScore = redisTemplate.opsForZSet()
                            .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
                    // 초기: 0, 구매 수량 2 * 2 = 4
                    assertThat(updatedScore).isEqualTo(4.0);
                });
    }

    @Test
    @DisplayName("조회와 구매가 함께 발생하면 점수가 모두 반영된다")
    void viewAndPurchase_BothScoresReflected() throws Exception {
        // given: 사용자 및 상품 준비
        User user = userRepository.save(User.builder().build());
        Long userId = user.getId();

        ChargePointRequest chargeRequest = new ChargePointRequest(1000000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andExpect(status().isOk());

        Product product = productRepository.save(Product.builder()
                .productName("인기 상품")
                .price(10000L)
                .viewCount(5)  // 기존 조회수 5
                .originalStockQuantity(100)
                .stockQuantity(90)  // 기존 판매 10개
                .build());

        // Redis 캐시 초기화: 초기 점수 = 5 + 10*2 = 25
        cacheService.initializeCache();

        Double initialScore = redisTemplate.opsForZSet()
                .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
        assertThat(initialScore).isEqualTo(25.0);

        // when: 상품 조회 3번
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/products/{productId}", product.getId()))
                    .andExpect(status().isOk());
        }

        // 장바구니 추가 및 결제 (수량 5)
        AddCartItemRequest cartRequest = new AddCartItemRequest(product.getId(), 5);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        OrderRequest orderRequest = new OrderRequest(userId, "테스터", "서울시", null);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .get("orderId")
                .asLong();

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andExpect(status().isOk());

        // then: 비동기 작업 완료 대기 후 최종 점수 확인
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Double finalScore = redisTemplate.opsForZSet()
                            .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
                    // 초기: 25, 조회 +3, 구매 +5*2=10 → 총 38
                    assertThat(finalScore).isEqualTo(38.0);
                });
    }

    @Test
    @DisplayName("캐시가 초기화되지 않았어도 점수 업데이트는 동작한다")
    void scoreUpdate_WorksWithoutInitialCache() throws Exception {
        // given: 캐시 초기화하지 않음 (Redis에 키 없음)
        Product product = productRepository.save(Product.builder()
                .productName("상품")
                .price(10000L)
                .viewCount(0)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());

        // 캐시에 키가 없는지 확인
        Double initialScore = redisTemplate.opsForZSet()
                .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
        assertThat(initialScore).isNull();

        // when: 상품 조회
        mockMvc.perform(get("/api/v1/products/{productId}", product.getId()))
                .andExpect(status().isOk());

        // then: 비동기로 키가 생성되고 점수가 반영됨
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Double score = redisTemplate.opsForZSet()
                            .score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
                    assertThat(score).isNotNull();
                    assertThat(score).isEqualTo(1.0);
                });
    }
}