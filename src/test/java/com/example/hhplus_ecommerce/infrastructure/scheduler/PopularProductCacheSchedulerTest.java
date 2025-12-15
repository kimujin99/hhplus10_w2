package com.example.hhplus_ecommerce.infrastructure.scheduler;

import com.example.hhplus_ecommerce.product.application.PopularProductCacheService;
import com.example.hhplus_ecommerce.product.infrastructure.scheduler.PopularProductCacheScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * PopularProductCacheScheduler 단위 테스트
 * <p>
 * 실제 스케줄링은 테스트하지 않고, 비즈니스 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class PopularProductCacheSchedulerTest {

    @Mock
    private PopularProductCacheService popularProductCacheService;

    @InjectMocks
    private PopularProductCacheScheduler scheduler;

    @Test
    @DisplayName("캐시 재계산 시 clearCache와 initializeCache가 순차적으로 호출된다")
    void refreshCache_Success() {
        // when
        scheduler.refreshCache();

        // then
        // InOrder를 사용하여 호출 순서 검증
        var inOrder = inOrder(popularProductCacheService);
        inOrder.verify(popularProductCacheService).clearCache();
        inOrder.verify(popularProductCacheService).initializeCache();
    }

    @Test
    @DisplayName("clearCache 실패 시에도 initializeCache가 호출되지 않는다")
    void refreshCache_ClearFails() {
        // given
        doThrow(new RuntimeException("Redis 연결 실패"))
                .when(popularProductCacheService).clearCache();

        // when & then
        try {
            scheduler.refreshCache();
        } catch (RuntimeException e) {
            // 예외 발생 확인
        }

        // clearCache 호출됨
        verify(popularProductCacheService).clearCache();
        // initializeCache는 호출 안 됨 (clearCache에서 예외 발생)
        verify(popularProductCacheService, never()).initializeCache();
    }
}