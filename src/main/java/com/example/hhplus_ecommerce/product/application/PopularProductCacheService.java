package com.example.hhplus_ecommerce.product.application;

import com.example.hhplus_ecommerce.product.infrastructure.dto.ProductScore;
import com.example.hhplus_ecommerce.product.infrastructure.redis.ProductPopularityRedisRepository;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 인기 상품 Redis 캐시 관리 서비스
 * <p>
 * Redis Sorted Set을 사용하여 인기 상품 점수를 관리합니다.
 * 점수 = 조회수 + (판매수 * 2)
 * <p>
 * ProductPopularityRedisRepository를 통해 Redis 접근을 추상화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularProductCacheService {

    private final ProductRepository productRepository;
    private final ProductPopularityRedisRepository popularityRedisRepository;

    /**
     * DB의 모든 상품 점수를 계산하여 Redis에 초기화합니다.
     * <p>
     * 서버 시작 시 또는 수동 API 호출로 실행됩니다.
     */
    @Transactional(readOnly = true)
    public void initializeCache() {
        log.info("인기 상품 캐시 초기화 시작");

        List<ProductScore> productScores = productRepository.findAllProductScores();

        // ProductPopularityRedisRepository를 통해 캐시 초기화
        popularityRedisRepository.initializeCache();
        productScores.forEach(score ->
            popularityRedisRepository.addToSortedSet(
                "popular:products",
                "product:" + score.id(),
                score.score()
            )
        );

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
            popularityRedisRepository.incrementViewScore(productId);
        } catch (Exception e) {
            log.error("조회수 점수 증가 실패: productId={}, error={}", productId, e.getMessage());
        }
    }

    /**
     * 결제 완료 시 구매된 상품들의 점수를 증가시킵니다.
     * <p>
     * 상품별 구매 수량 * 2만큼 점수를 증가시킵니다.
     *
     * @param productId 구매된 상품 ID
     * @param quantity 구매 수량
     */
    public void incrementPurchaseScore(Long productId, int quantity) {
        try {
            popularityRedisRepository.incrementPurchaseScore(productId, quantity);
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
        try {
            return popularityRedisRepository.getTopProductIds(limit);
        } catch (Exception e) {
            log.error("인기 상품 조회 실패: limit={}, error={}", limit, e.getMessage());
            return List.of();
        }
    }

    /**
     * 캐시를 초기화(삭제)합니다.
     */
    public void clearCache() {
        try {
            popularityRedisRepository.initializeCache();
            log.info("인기 상품 캐시 삭제 완료");
        } catch (Exception e) {
            log.error("캐시 삭제 실패: error={}", e.getMessage());
        }
    }
}