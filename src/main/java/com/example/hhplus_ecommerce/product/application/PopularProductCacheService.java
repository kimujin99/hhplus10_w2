package com.example.hhplus_ecommerce.product.application;

import com.example.hhplus_ecommerce.product.infrastructure.dto.ProductScore;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 인기 상품 Redis 캐시 관리 서비스
 * <p>
 * Redis Sorted Set을 사용하여 인기 상품 점수를 관리합니다.
 * 점수 = 조회수 + (판매수 * 2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularProductCacheService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String POPULAR_PRODUCTS_KEY = "popular:products";

    /**
     * DB의 모든 상품 점수를 계산하여 Redis에 초기화합니다.
     * <p>
     * Redis Pipeline을 사용하여 대량 데이터를 효율적으로 삽입합니다.
     * 서버 시작 시 또는 수동 API 호출로 실행됩니다.
     */
    @Transactional(readOnly = true)
    public void initializeCache() {
        log.info("인기 상품 캐시 초기화 시작");

        List<ProductScore> productScores = productRepository.findAllProductScores();

        // Redis Pipeline 사용 (네트워크 왕복 최소화)
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            productScores.forEach(score -> {
                String key = POPULAR_PRODUCTS_KEY;
                String member = "product:" + score.id();
                double scoreValue = score.score();

                connection.zSetCommands().zAdd(
                        key.getBytes(),
                        scoreValue,
                        member.getBytes()
                );
            });
            return null; // Pipeline 실행 시 null 반환
        });

        log.info("인기 상품 캐시 초기화 완료: {} 개 상품", productScores.size());
    }

    /**
     * 상품 조회 시 점수를 +1 증가시킵니다.
     * <p>
     * 비동기로 실행되어 조회 성능에 영향을 주지 않습니다.
     *
     * @param productId 조회된 상품 ID
     */
    @Async("asyncExecutor")
    public void incrementViewScore(Long productId) {
        try {
            String member = "product:" + productId;
            redisTemplate.opsForZSet().incrementScore(POPULAR_PRODUCTS_KEY, member, 1.0);
            log.debug("조회수 점수 증가: productId={}", productId);
        } catch (Exception e) {
            log.error("조회수 점수 증가 실패: productId={}, error={}", productId, e.getMessage());
        }
    }

    /**
     * 결제 완료 시 구매된 상품들의 점수를 증가시킵니다.
     * <p>
     * 비동기로 실행되어 결제 성능에 영향을 주지 않습니다.
     * 상품별 구매 수량 * 2만큼 점수를 증가시킵니다.
     *
     * @param productId 구매된 상품 ID
     * @param quantity 구매 수량
     */
    public void incrementPurchaseScore(Long productId, int quantity) {
        try {
            String member = "product:" + productId;
            double scoreIncrement = quantity * 2.0;
            redisTemplate.opsForZSet().incrementScore(POPULAR_PRODUCTS_KEY, member, scoreIncrement);
            log.debug("구매 점수 증가: productId={}, quantity={}, scoreIncrement={}",
                    productId, quantity, scoreIncrement);
        } catch (Exception e) {
            log.error("구매 점수 증가 실패: productId={}, quantity={}, error={}",
                    productId, quantity, e.getMessage());
        }
    }

    /**
     * Redis 캐시에서 상위 N개의 인기 상품 ID를 조회합니다.
     *
     * @param limit 조회할 상품 개수
     * @return 상품 ID 목록 (점수 높은 순)
     */
    public List<Long> getTopProductIds(int limit) {
        // ZREVRANGE: 점수 높은 순으로 조회
        var members = redisTemplate.opsForZSet()
                .reverseRange(POPULAR_PRODUCTS_KEY, 0, limit - 1);

        if (members == null || members.isEmpty()) {
            log.warn("Redis 캐시가 비어있습니다. 초기화가 필요합니다.");
            return List.of();
        }

        return members.stream()
                .map(member -> Long.parseLong(member.replace("product:", "")))
                .toList();
    }

    /**
     * 캐시를 초기화(삭제)합니다.
     */
    public void clearCache() {
        redisTemplate.delete(POPULAR_PRODUCTS_KEY);
        log.info("인기 상품 캐시 삭제 완료");
    }
}