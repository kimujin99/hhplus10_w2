package com.example.hhplus_ecommerce.product.infrastructure.redis;

import com.example.hhplus_ecommerce.common.infrastructure.redis.AbstractRedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 상품 인기도 Redis 저장소
 * <p>
 * 상품의 인기도 점수를 Redis Sorted Set에 저장하고 관리합니다.
 * - 조회수 점수: +1
 * - 판매수 점수: +2
 */
@Slf4j
@Repository
public class ProductPopularityRedisRepository extends AbstractRedisRepository {

    private static final String POPULAR_PRODUCTS_KEY = "popular:products";

    public ProductPopularityRedisRepository(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    /**
     * 상품의 조회수 점수를 증가시킵니다. (+1)
     *
     * @param productId 상품 ID
     */
    public void incrementViewScore(Long productId) {
        String member = "product:" + productId;
        incrementScore(POPULAR_PRODUCTS_KEY, member, 1.0);
        log.debug("조회수 점수 증가: productId={}", productId);
    }

    /**
     * 상품의 판매수 점수를 증가시킵니다. (+quantity * 2)
     *
     * @param productId 상품 ID
     * @param quantity  구매 수량
     */
    public void incrementPurchaseScore(Long productId, int quantity) {
        String member = "product:" + productId;
        double scoreIncrement = quantity * 2.0;
        incrementScore(POPULAR_PRODUCTS_KEY, member, scoreIncrement);
        log.debug("구매 점수 증가: productId={}, quantity={}, scoreIncrement={}",
                productId, quantity, scoreIncrement);
    }

    /**
     * 상위 N개의 인기 상품 ID를 조회합니다. (점수 높은 순)
     *
     * @param limit 조회할 상품 개수
     * @return 상품 ID 목록
     */
    public List<Long> getTopProductIds(int limit) {
        List<String> members = getTopMembers(POPULAR_PRODUCTS_KEY, limit);

        if (members.isEmpty()) {
            log.warn("Redis 캐시가 비어있습니다. 초기화가 필요합니다.");
            return List.of();
        }

        return members.stream()
                .map(member -> Long.parseLong(member.replace("product:", "")))
                .toList();
    }

    /**
     * 모든 상품의 점수를 초기화합니다.
     */
    public void initializeCache() {
        delete(POPULAR_PRODUCTS_KEY);
        log.info("상품 인기도 캐시 초기화 완료");
    }

    /**
     * 특정 상품의 모든 점수를 초기화합니다.
     *
     * @param productId 상품 ID
     */
    public void resetProductScore(Long productId) {
        String member = "product:" + productId;
        redisTemplate.opsForZSet().remove(POPULAR_PRODUCTS_KEY, member);
        log.debug("상품 점수 초기화: productId={}", productId);
    }
}