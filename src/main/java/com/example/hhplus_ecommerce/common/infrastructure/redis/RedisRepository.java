package com.example.hhplus_ecommerce.common.infrastructure.redis;

import java.util.List;
import java.util.Set;

/**
 * Redis 접근을 위한 추상화 인터페이스
 * <p>
 * 각 도메인의 Redis 저장소는 이 인터페이스를 구현하여
 * Redis 접근 로직을 캡슐화합니다.
 */
public interface RedisRepository<T> {

    /**
     * 주어진 키에 값을 저장합니다.
     */
    void set(String key, T value);

    /**
     * 주어진 키의 값을 조회합니다.
     */
    T get(String key);

    /**
     * 주어진 키가 존재하는지 확인합니다.
     */
    boolean exists(String key);

    /**
     * 주어진 키를 삭제합니다.
     */
    void delete(String key);

    /**
     * 모든 키를 삭제합니다.
     */
    void clear();

    /**
     * Sorted Set에 점수를 증가시킵니다.
     */
    double incrementScore(String key, String member, double increment);

    /**
     * Sorted Set의 상위 N개 요소를 점수 높은 순으로 조회합니다.
     */
    List<String> getTopMembers(String key, long limit);

    /**
     * Sorted Set에 요소를 추가합니다.
     */
    void addToSortedSet(String key, String member, double score);

    /**
     * Sorted Set의 모든 요소를 조회합니다.
     */
    Set<String> getSetMembers(String key);
}