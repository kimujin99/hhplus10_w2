package com.example.hhplus_ecommerce.infrastructure.scheduler;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PopularProductCacheScheduler 통합 테스트
 * <p>
 * 실제 Redis와 DB를 사용하여 스케줄러의 캐시 재계산 기능을 검증합니다.
 * 스케줄링 자체는 테스트하지 않고, refreshCache() 메서드를 직접 호출합니다.
 */
class PopularProductCacheSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PopularProductCacheScheduler scheduler;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String POPULAR_PRODUCTS_KEY = "popular:products";

    @Test
    @DisplayName("refreshCache 호출 시 DB 데이터를 기반으로 Redis 캐시가 재구성된다")
    void refreshCache_Success() {
        // given: DB에 상품 3개 생성
        Product p1 = productRepository.save(Product.builder()
                .productName("상품1")
                .price(1000L)
                .viewCount(10)
                .originalStockQuantity(100)
                .stockQuantity(90) // 판매 10개
                .build());

        Product p2 = productRepository.save(Product.builder()
                .productName("상품2")
                .price(2000L)
                .viewCount(5)
                .originalStockQuantity(100)
                .stockQuantity(80) // 판매 20개
                .build());

        Product p3 = productRepository.save(Product.builder()
                .productName("상품3")
                .price(3000L)
                .viewCount(0)
                .originalStockQuantity(100)
                .stockQuantity(100) // 판매 0개
                .build());

        // when: 캐시 재계산
        scheduler.refreshCache();

        // then: Redis에 3개 상품이 모두 캐싱됨
        Long cacheSize = redisTemplate.opsForZSet().zCard(POPULAR_PRODUCTS_KEY);
        assertThat(cacheSize).isEqualTo(3);

        // 점수 검증
        // p1: viewCount(10) + sales(10) * 2 = 30
        Double p1Score = redisTemplate.opsForZSet().score(POPULAR_PRODUCTS_KEY, "product:" + p1.getId());
        assertThat(p1Score).isEqualTo(30.0);

        // p2: viewCount(5) + sales(20) * 2 = 45
        Double p2Score = redisTemplate.opsForZSet().score(POPULAR_PRODUCTS_KEY, "product:" + p2.getId());
        assertThat(p2Score).isEqualTo(45.0);

        // p3: viewCount(0) + sales(0) * 2 = 0
        Double p3Score = redisTemplate.opsForZSet().score(POPULAR_PRODUCTS_KEY, "product:" + p3.getId());
        assertThat(p3Score).isEqualTo(0.0);
    }

    @Test
    @Transactional  // incrementViewCount는 @Modifying 쿼리이므로 트랜잭션 필요
    @DisplayName("refreshCache 재호출 시 기존 캐시가 삭제되고 새로운 캐시로 대체된다")
    void refreshCache_OverwritesExistingCache() {
        // given: 초기 상품 생성 및 캐싱
        Product product = productRepository.save(Product.builder()
                .productName("테스트 상품")
                .price(1000L)
                .viewCount(10)
                .originalStockQuantity(100)
                .stockQuantity(90)
                .build());

        scheduler.refreshCache();

        // 초기 점수 확인: 10 + 10*2 = 30
        Double initialScore = redisTemplate.opsForZSet().score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
        assertThat(initialScore).isEqualTo(30.0);

        // when: DB에서 조회수 증가 시뮬레이션
        productRepository.incrementViewCount(product.getId());

        // 캐시 재계산
        scheduler.refreshCache();

        // then: 새로운 점수로 업데이트됨: 11 + 10*2 = 31
        Double updatedScore = redisTemplate.opsForZSet().score(POPULAR_PRODUCTS_KEY, "product:" + product.getId());
        assertThat(updatedScore).isEqualTo(31.0);
    }

    @Test
    @DisplayName("DB에 상품이 없으면 캐시도 비어있다")
    void refreshCache_EmptyDatabase() {
        // given: DB에 상품 없음 (cleanupDatabase로 이미 삭제됨)

        // when: 캐시 재계산
        scheduler.refreshCache();

        // then: Redis 캐시도 비어있음
        Long cacheSize = redisTemplate.opsForZSet().zCard(POPULAR_PRODUCTS_KEY);
        assertThat(cacheSize).isEqualTo(0);
    }
}