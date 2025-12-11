package com.example.hhplus_ecommerce.infrastructure.scheduler;

import com.example.hhplus_ecommerce.application.service.PopularProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 인기 상품 캐시 갱신 스케줄러
 * <p>
 * 매일 자정에 DB 기준으로 캐시를 재계산하여 정합성을 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductCacheScheduler {

    private final PopularProductCacheService popularProductCacheService;

    /**
     * 매일 오전 3시에 캐시 재계산
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledRefreshCache() {
        log.info("스케줄러 실행 - 인기 상품 캐시 재계산 시작");
        refreshCache();
        log.info("스케줄러 실행 - 인기 상품 캐시 재계산 완료");
    }

    /**
     * 캐시를 재계산합니다.
     * <p>
     * 테스트 가능하도록 public으로 분리했습니다.
     * 스케줄러 또는 수동 호출 모두 가능합니다.
     */
    public void refreshCache() {
        popularProductCacheService.clearCache();
        popularProductCacheService.initializeCache();
    }
}