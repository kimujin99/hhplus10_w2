package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.CouponResponse;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.IssueCouponRequest;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.UserCouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@EnableRetry
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String COUPON_ISSUED_KEY_PREFIX = "coupon:";
    private static final String COUPON_ISSUED_KEY_SUFFIX = ":issued";
    private static final String COUPON_USER_KEY_FORMAT = "coupon:%d:user:%d";
    private static final Duration COUPON_USER_KEY_TTL = Duration.ofDays(30);

    public List<CouponResponse> getCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        return CouponResponse.fromList(coupons);
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);
        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰을 발급합니다.
     * <p>
     * 사용자에게 쿠폰을 발급하며, 쿠폰 재고 확인, 중복 발급 검증 등을 수행합니다.
     * 선착순 쿠폰의 경우 재고가 소진되면 발급이 불가능합니다.
     * <p>
     * 동시성 제어:
     * - Redis INCR + SET을 사용한 원자적 연산으로 동시성 제어
     * - Redis INCR: 발급 수량을 원자적으로 증가시켜 선착순 보장
     * - Redis SETNX: 중복 발급 방지 (같은 사용자가 같은 쿠폰 중복 발급 불가)
     * - 분산 환경에서 락 없이 고성능 처리 가능
     *
     * @param userId 쿠폰을 발급받을 사용자 ID
     * @param request 쿠폰 발급 요청 정보 (쿠폰 ID)
     * @return 발급된 사용자 쿠폰 정보
     * @throws ConflictException 쿠폰이 이미 발급되었거나 재고가 없는 경우
     */
    @Transactional
    public UserCouponResponse issueCoupon(Long userId, IssueCouponRequest request) {
        Long couponId = request.couponId();

        // Redis 키 생성
        String issuedKey = COUPON_ISSUED_KEY_PREFIX + couponId + COUPON_ISSUED_KEY_SUFFIX;
        String userKey = String.format(COUPON_USER_KEY_FORMAT, couponId, userId);

        // 1. Redis SETNX로 중복 발급 체크 (원자적 연산)
        Boolean isNewUser = redisTemplate.opsForValue().setIfAbsent(userKey, "1", COUPON_USER_KEY_TTL);
        if (Boolean.FALSE.equals(isNewUser)) {
            log.warn("쿠폰 중복 발급 시도: userId={}, couponId={}", userId, couponId);
            throw new ConflictException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        try {
            // 2. Redis INCR로 발급 수량 증가 (원자적 연산)
            Long currentIssued = redisTemplate.opsForValue().increment(issuedKey);
            if (currentIssued == null) {
                log.error("Redis INCR 실패: issuedKey={}", issuedKey);
                throw new ConflictException(CouponErrorCode.COUPON_SOLD_OUT, "쿠폰 발급 처리 중 오류가 발생했습니다.");
            }

            log.info("쿠폰 발급 시도: userId={}, couponId={}, currentIssued={}", userId, couponId, currentIssued);

            // 3. Redis에서 총 수량 조회 (먼저 빠른 검증)
            String totalKey = COUPON_ISSUED_KEY_PREFIX + couponId + ":total";
            String totalQuantityStr = redisTemplate.opsForValue().get(totalKey);
            Integer totalQuantity;

            if (totalQuantityStr != null) {
                // Redis에 캐시된 값 사용
                totalQuantity = Integer.parseInt(totalQuantityStr);
            } else {
                // Redis에 없으면 DB에서 조회하고 캐싱
                Coupon coupon = couponRepository.findByIdOrThrow(couponId);
                totalQuantity = coupon.getTotalQuantity();
                redisTemplate.opsForValue().set(totalKey, String.valueOf(totalQuantity));
            }

            // 4. 재고 검증 (Redis 기준)
            if (currentIssued > totalQuantity) {
                log.warn("쿠폰 재고 부족: couponId={}, currentIssued={}, totalQuantity={}",
                    couponId, currentIssued, totalQuantity);

                // 재고 초과 시 Redis 카운터 롤백
                redisTemplate.opsForValue().decrement(issuedKey);
                // 중복 방지 키도 삭제
                redisTemplate.delete(userKey);

                throw new ConflictException(CouponErrorCode.COUPON_SOLD_OUT);
            }

            // 5. DB에서 사용자 및 쿠폰 조회 (최종 검증 및 저장용)
            userRepository.findByIdOrThrow(userId);
            Coupon coupon = couponRepository.findByIdOrThrow(couponId);

            // 6. 쿠폰 유효기간 검증 및 발급
            coupon.issue();
            couponRepository.save(coupon);

            // 7. UserCoupon 저장
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(userId)
                    .coupon(coupon)
                    .build();
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            log.info("쿠폰 발급 성공: userId={}, couponId={}, issuedQuantity={}",
                userId, couponId, coupon.getIssuedQuantity());

            return UserCouponResponse.from(savedUserCoupon, coupon);

        } catch (ConflictException e) {
            // 이미 롤백 처리된 경우 그대로 전파
            throw e;
        } catch (Exception e) {
            // 예상치 못한 오류 발생 시 Redis 롤백
            log.error("쿠폰 발급 중 예외 발생: userId={}, couponId={}", userId, couponId, e);
            redisTemplate.opsForValue().decrement(issuedKey);
            redisTemplate.delete(userKey);
            throw e;
        }
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        userRepository.findByIdOrThrow(userId);
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        return userCoupons.stream()
                .map(userCoupon -> UserCouponResponse.from(userCoupon, userCoupon.getCoupon()))
                .toList();
    }

    /**
     * Redis의 쿠폰 발급 카운터를 초기화합니다.
     *
     * @param couponId 쿠폰 ID
     */
    public void initializeCouponCache(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);

        String issuedKey = COUPON_ISSUED_KEY_PREFIX + couponId + COUPON_ISSUED_KEY_SUFFIX;
        String totalKey = COUPON_ISSUED_KEY_PREFIX + couponId + ":total";

        // Redis Pipeline 사용 (네트워크 왕복 최소화)
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.stringCommands().set(
                issuedKey.getBytes(),
                String.valueOf(coupon.getIssuedQuantity()).getBytes()
            );
            connection.stringCommands().set(
                totalKey.getBytes(),
                String.valueOf(coupon.getTotalQuantity()).getBytes()
            );
            return null; // Pipeline 실행 시 null 반환
        });

        log.info("쿠폰 Redis 카운터 초기화: couponId={}, issuedQuantity={}, totalQuantity={}",
            couponId, coupon.getIssuedQuantity(), coupon.getTotalQuantity());
    }

    /**
     * DB의 모든 활성 쿠폰 정보를 Redis에 일괄 초기화합니다.
     * <p>
     * Redis Pipeline을 사용하여 대량 데이터를 효율적으로 삽입합니다.
     * 서버 시작 시 또는 수동 API 호출로 실행됩니다.
     */
    @Transactional(readOnly = true)
    public void initializeAllCouponCache() {
        log.info("전체 쿠폰 캐시 초기화 시작");

        List<Coupon> activeCoupons = couponRepository.findAll();

        // Redis Pipeline 사용 (네트워크 왕복 최소화)
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            activeCoupons.forEach(coupon -> {
                String issuedKey = COUPON_ISSUED_KEY_PREFIX + coupon.getId() + COUPON_ISSUED_KEY_SUFFIX;
                String totalKey = COUPON_ISSUED_KEY_PREFIX + coupon.getId() + ":total";

                connection.stringCommands().set(
                    issuedKey.getBytes(),
                    String.valueOf(coupon.getIssuedQuantity()).getBytes()
                );
                connection.stringCommands().set(
                    totalKey.getBytes(),
                    String.valueOf(coupon.getTotalQuantity()).getBytes()
                );
            });
            return null; // Pipeline 실행 시 null 반환
        });

        log.info("전체 쿠폰 캐시 초기화 완료: {} 개 쿠폰", activeCoupons.size());
    }

    /**
     * 쿠폰 캐시를 초기화(삭제)합니다.
     */
    public void clearCache() {
        Set<String> keys = redisTemplate.keys(COUPON_ISSUED_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("쿠폰 캐시 삭제 완료");
    }
}