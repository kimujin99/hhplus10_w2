package com.example.hhplus_ecommerce.coupon.infrastructure.redis;

import com.example.hhplus_ecommerce.common.infrastructure.redis.AbstractRedisRepository;
import com.example.hhplus_ecommerce.coupon.infrastructure.dto.CouponIssueQueueItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 쿠폰 선착순 발급 Redis 저장소
 * <p>
 * Redis를 사용한 선착순 쿠폰 발급 시스템:
 * - Set: 발급받은 사용자 목록 (중복 발급 방지)
 * - String: 재고 수량
 * - List: 발급 대기 큐
 */
@Slf4j
@Repository
public class CouponIssueRedisRepository extends AbstractRedisRepository {

    private static final String COUPON_USER_SET_KEY = "coupon:user:%d";      // Set: 발급받은 사용자 목록
    private static final String COUPON_STOCK_KEY = "coupon:stock:%d";         // String: 재고 수량
    private static final String COUPON_QUEUE_KEY = "coupon:issue:queue:%d";   // List: 발급 대기 큐

    public CouponIssueRedisRepository(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    /**
     * 쿠폰을 발급받을 사용자를 Set에 추가합니다. (중복 발급 방지)
     * <p>
     * SADD는 원자적 연산으로 동시성 문제를 해결합니다.
     *
     * @param couponId 쿠폰 ID
     * @param userId   발급받을 사용자 ID
     * @return 추가 성공 여부 (0 = 이미 존재, 1 = 새로 추가)
     */
    public Long addUserToIssuedSet(Long couponId, Long userId) {
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        Long added = redisTemplate.opsForSet().add(userSetKey, userId.toString());
        log.debug("쿠폰 발급 사용자 추가: couponId={}, userId={}, added={}", couponId, userId, added);
        return added;
    }

    /**
     * 특정 사용자를 발급 사용자 Set에서 제거합니다. (롤백용)
     *
     * @param couponId 쿠폰 ID
     * @param userId   제거할 사용자 ID
     */
    public void removeUserFromIssuedSet(Long couponId, Long userId) {
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        redisTemplate.opsForSet().remove(userSetKey, userId.toString());
        log.debug("쿠폰 발급 사용자 제거: couponId={}, userId={}", couponId, userId);
    }

    /**
     * 쿠폰의 현재 발급 수를 반환합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 발급 수
     */
    public Long getIssuedCount(Long couponId) {
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        Long size = redisTemplate.opsForSet().size(userSetKey);
        return size != null ? size : 0L;
    }

    /**
     * 쿠폰의 재고를 설정합니다.
     *
     * @param couponId 쿠폰 ID
     * @param stock    재고 수량
     */
    public void setStock(Long couponId, Long stock) {
        String stockKey = String.format(COUPON_STOCK_KEY, couponId);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        log.debug("쿠폰 재고 설정: couponId={}, stock={}", couponId, stock);
    }

    /**
     * 쿠폰의 재고를 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 재고 수량 (없으면 null)
     */
    public Long getStock(Long couponId) {
        String stockKey = String.format(COUPON_STOCK_KEY, couponId);
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        return stockStr != null ? Long.parseLong(stockStr) : null;
    }

    /**
     * 발급 대기 큐에 항목을 추가합니다.
     *
     * @param couponId  쿠폰 ID
     * @param queueItem 발급 대기 항목
     */
    public void addToQueue(Long couponId, CouponIssueQueueItem queueItem) {
        String queueKey = String.format(COUPON_QUEUE_KEY, couponId);
        redisTemplate.opsForList().rightPush(queueKey, queueItem.toJson());
        log.debug("쿠폰 발급 큐 추가: couponId={}, queueItem={}", couponId, queueItem);
    }

    /**
     * 발급 대기 큐의 크기를 반환합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 큐 항목 수
     */
    public Long getQueueSize(Long couponId) {
        String queueKey = String.format(COUPON_QUEUE_KEY, couponId);
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0L;
    }

    /**
     * 쿠폰 관련 모든 Redis 키를 조회합니다.
     *
     * @return 쿠폰 관련 키 Set
     */
    public Set<String> getAllCouponKeys() {
        Set<String> keys = redisTemplate.keys("coupon:*");
        return keys != null ? keys : Set.of();
    }

    /**
     * 특정 쿠폰의 발급 사용자 Set을 초기화합니다.
     *
     * @param couponId 쿠폰 ID
     */
    public void clearIssuedUserSet(Long couponId) {
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        redisTemplate.delete(userSetKey);
        log.debug("쿠폰 발급 사용자 Set 초기화: couponId={}", couponId);
    }

    /**
     * 모든 쿠폰 캐시를 삭제합니다.
     */
    public void clearAll() {
        Set<String> keys = getAllCouponKeys();
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("쿠폰 캐시 모두 삭제 완료: {} 개 키", keys.size());
    }

    /**
     * 특정 쿠폰의 발급 큐 키를 반환합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 큐 키
     */
    public String getQueueKey(Long couponId) {
        return String.format(COUPON_QUEUE_KEY, couponId);
    }

    /**
     * 특정 쿠폰의 발급 사용자 목록을 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 발급받은 사용자 ID 목록
     */
    public Set<String> getIssuedUserIds(Long couponId) {
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        Set<String> members = redisTemplate.opsForSet().members(userSetKey);
        return members != null ? members : Set.of();
    }

    /**
     * 여러 사용자를 발급 사용자 Set에 한 번에 추가합니다. (초기화용)
     *
     * @param couponId 쿠폰 ID
     * @param userIds 추가할 사용자 ID 목록
     */
    public void bulkAddUserToIssuedSet(Long couponId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        String[] userIdStrings = userIds.stream()
            .map(String::valueOf)
            .toArray(String[]::new);
        redisTemplate.opsForSet().add(userSetKey, userIdStrings);
        log.debug("쿠폰 발급 사용자 일괄 추가: couponId={}, count={}", couponId, userIds.size());
    }
}