package com.example.hhplus_ecommerce.infrastructure.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산락을 적용하기 위한 어노테이션
 * <p>
 * Redisson RLock을 사용하여 메서드 실행 전에 락을 획득하고, 실행 후 자동으로 락을 해제합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락의 이름 (키)
     * SpEL 표현식 사용 가능 (예: "payment:#{#orderId}")
     */
    String key();

    /**
     * 락 획득을 시도하는 최대 대기 시간 (기본값: 5초)
     */
    long waitTime() default 5L;

    /**
     * 락을 획득한 후 자동으로 해제되는 시간 (기본값: 3초)
     * Watchdog가 자동으로 갱신하므로 일반적으로 짧게 설정
     */
    long leaseTime() default 3L;

    /**
     * 시간 단위 (기본값: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}