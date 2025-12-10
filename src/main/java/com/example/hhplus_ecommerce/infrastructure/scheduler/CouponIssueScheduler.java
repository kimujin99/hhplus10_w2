package com.example.hhplus_ecommerce.infrastructure.scheduler;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.dto.CouponIssueQueueItem;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 쿠폰 발급 큐 처리 스케줄러
 * <p>
 * Redis 큐에 쌓인 쿠폰 발급 요청을 1초마다 처리하여 DB에 저장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueScheduler {

    private final StringRedisTemplate redisTemplate;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    private static final String COUPON_QUEUE_KEY_PATTERN = "coupon:issue:queue:*";
    private static final int BATCH_SIZE = 100;

    /**
     * 1초마다 쿠폰 발급 큐를 처리합니다.
     */
    @Scheduled(fixedDelay = 1000)
    public void processQueue() {
        // 모든 쿠폰 큐 키 조회
        var queueKeys = redisTemplate.keys(COUPON_QUEUE_KEY_PATTERN);
        if (queueKeys == null || queueKeys.isEmpty()) {
            return;
        }

        for (String queueKey : queueKeys) {
            processQueueForKey(queueKey);
        }
    }

    /**
     * 특정 쿠폰 큐를 처리합니다.
     */
    @Transactional
    public void processQueueForKey(String queueKey) {
        List<CouponIssueQueueItem> items = new ArrayList<>();

        // LPOP으로 BATCH_SIZE만큼 가져오기
        for (int i = 0; i < BATCH_SIZE; i++) {
            String json = redisTemplate.opsForList().leftPop(queueKey);
            if (json == null) {
                break;
            }
            try {
                items.add(CouponIssueQueueItem.fromJson(json));
            } catch (Exception e) {
                log.error("쿠폰 발급 큐 아이템 파싱 실패: {}", json, e);
            }
        }

        if (items.isEmpty()) {
            return;
        }

        log.info("쿠폰 발급 큐 처리 시작: queueKey={}, itemCount={}", queueKey, items.size());

        // 쿠폰 ID별로 그룹화
        Map<Long, List<CouponIssueQueueItem>> groupedByCouponId = items.stream()
            .collect(Collectors.groupingBy(CouponIssueQueueItem::couponId));

        for (Map.Entry<Long, List<CouponIssueQueueItem>> entry : groupedByCouponId.entrySet()) {
            Long couponId = entry.getKey();
            List<CouponIssueQueueItem> couponItems = entry.getValue();

            try {
                processCouponBatch(couponId, couponItems);
            } catch (Exception e) {
                log.error("쿠폰 배치 처리 실패: couponId={}", couponId, e);
                // 실패한 아이템은 다시 큐에 넣기 (재시도)
                for (CouponIssueQueueItem item : couponItems) {
                    redisTemplate.opsForList().rightPush(queueKey, item.toJson());
                }
            }
        }
    }

    /**
     * 쿠폰 배치를 처리합니다.
     */
    private void processCouponBatch(Long couponId, List<CouponIssueQueueItem> items) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);

        List<UserCoupon> userCoupons = new ArrayList<>();
        for (CouponIssueQueueItem item : items) {
            // 이미 발급된 쿠폰인지 확인 (중복 방지)
            if (userCouponRepository.findByUserIdAndCoupon_Id(item.userId(), couponId).isPresent()) {
                log.warn("이미 DB에 저장된 쿠폰: userId={}, couponId={}", item.userId(), couponId);
                continue;
            }

            UserCoupon userCoupon = UserCoupon.builder()
                .userId(item.userId())
                .coupon(coupon)
                .build();
            userCoupons.add(userCoupon);
        }

        if (!userCoupons.isEmpty()) {
            // UserCoupon 일괄 저장
            userCouponRepository.saveAll(userCoupons);

            // Coupon issuedQuantity 업데이트
            coupon.incrementIssuedQuantity(userCoupons.size());
            couponRepository.save(coupon);

            log.info("쿠폰 발급 완료: couponId={}, count={}", couponId, userCoupons.size());
        }
    }
}
