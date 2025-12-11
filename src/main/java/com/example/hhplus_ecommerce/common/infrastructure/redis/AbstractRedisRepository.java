package com.example.hhplus_ecommerce.common.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * RedisRepository의 기본 구현 추상 클래스
 * <p>
 * StringRedisTemplate를 사용하여 Redis 작업을 수행합니다.
 * 각 도메인별 RedisRepository는 이 클래스를 상속받아 구현합니다.
 */
public abstract class AbstractRedisRepository implements RedisRepository<String> {

    protected final StringRedisTemplate redisTemplate;

    public AbstractRedisRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void clear() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Override
    public double incrementScore(String key, String member, double increment) {
        Double score = redisTemplate.opsForZSet().incrementScore(key, member, increment);
        return score != null ? score : 0.0;
    }

    @Override
    public List<String> getTopMembers(String key, long limit) {
        var members = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        return members != null ? members.stream().toList() : List.of();
    }

    @Override
    public void addToSortedSet(String key, String member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    @Override
    public Set<String> getSetMembers(String key) {
        var members = redisTemplate.opsForZSet().range(key, 0, -1);
        return members != null ? members : Set.of();
    }
}