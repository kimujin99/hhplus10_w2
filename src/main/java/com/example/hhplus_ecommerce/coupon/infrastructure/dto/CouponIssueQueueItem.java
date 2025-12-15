package com.example.hhplus_ecommerce.coupon.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;

/**
 * 쿠폰 발급 큐에 저장되는 아이템
 * <p>
 * Redis List에 JSON 형태로 저장되며, 스케줄러에 의해 처리됩니다.
 */
public record CouponIssueQueueItem(
    @JsonProperty("userId") Long userId,
    @JsonProperty("couponId") Long couponId,
    @JsonProperty("queuedAt") Instant queuedAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    /**
     * JSON 역직렬화를 위한 생성자
     */
    @JsonCreator
    public CouponIssueQueueItem {
    }

    /**
     * 새로운 큐 아이템을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 현재 시간이 기록된 큐 아이템
     */
    public static CouponIssueQueueItem create(Long userId, Long couponId) {
        return new CouponIssueQueueItem(userId, couponId, Instant.now());
    }

    /**
     * 객체를 JSON 문자열로 직렬화합니다.
     *
     * @return JSON 문자열
     * @throws IllegalStateException JSON 직렬화 실패 시
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("쿠폰 발급 큐 아이템 직렬화 실패", e);
        }
    }

    /**
     * JSON 문자열을 객체로 역직렬화합니다.
     *
     * @param json JSON 문자열
     * @return CouponIssueQueueItem 객체
     * @throws IllegalStateException JSON 역직렬화 실패 시
     */
    public static CouponIssueQueueItem fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, CouponIssueQueueItem.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("쿠폰 발급 큐 아이템 역직렬화 실패: " + json, e);
        }
    }
}
