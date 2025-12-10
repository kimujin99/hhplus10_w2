package com.example.hhplus_ecommerce.infrastructure.config;

import com.example.hhplus_ecommerce.application.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 쿠폰 캐시를 초기화하는 컴포넌트
 * <p>
 * 서버 시작 시점에 DB의 쿠폰 정보를 Redis에 캐싱합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponCacheInitializer {

    private final CouponService couponService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("애플리케이션 시작 - 쿠폰 캐시 초기화 시작");
        try {
            couponService.initializeAllCouponCache();
            log.info("애플리케이션 시작 - 쿠폰 캐시 초기화 완료");
        } catch (Exception e) {
            log.error("쿠폰 캐시 초기화 실패", e);
        }
    }
}
