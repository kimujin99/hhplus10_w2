package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.infrastructure.dto.CouponIssueQueueItem;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.CouponResponse;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.IssueCouponRequest;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.UserCouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    // Redis 키 패턴
    private static final String COUPON_USER_SET_KEY = "coupon:user:%d";      // Set: 발급받은 사용자 목록
    private static final String COUPON_STOCK_KEY = "coupon:stock:%d";         // String: 재고 수량
    private static final String COUPON_QUEUE_KEY = "coupon:issue:queue:%d";   // List: 발급 대기 큐

    public List<CouponResponse> getCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        return CouponResponse.fromList(coupons);
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);
        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰 발급을 요청합니다. (비동기 처리)
     * <p>
     * Redis를 사용한 선착순 쿠폰 발급 시스템:
     * 1. Set에 userId 추가 (SADD - 원자적 연산, 중복 발급 방지)
     * 2. 재고 확인 (Set size vs stock)
     * 3. 발급 큐에 추가 (List RPUSH)
     * 4. 사용자에게 즉시 응답 (실제 DB 저장은 스케줄러가 처리)
     * <p>
     * 동시성 제어:
     * - Redis SADD: 원자적 연산으로 중복 발급 방지
     * - Redis SCARD: Set 크기로 현재 발급 수 확인
     * - 실패 시 SREM으로 롤백 (원자적)
     *
     * @param userId 쿠폰을 발급받을 사용자 ID
     * @param request 쿠폰 발급 요청 정보 (쿠폰 ID)
     * @throws NotFoundException 쿠폰이 존재하지 않는 경우
     * @throws ConflictException 쿠폰이 이미 발급되었거나 재고가 없는 경우
     */
    public void issueCoupon(Long userId, IssueCouponRequest request) {
        Long couponId = request.couponId();

        // Redis 키 생성
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);
        String stockKey = String.format(COUPON_STOCK_KEY, couponId);
        String queueKey = String.format(COUPON_QUEUE_KEY, couponId);

        // 1. Set에 userId 추가 (SADD - 원자적, 중복 시 0 반환)
        Long added = redisTemplate.opsForSet().add(userSetKey, userId.toString());
        if (added == null || added == 0) {
            log.warn("쿠폰 중복 발급 시도: userId={}, couponId={}", userId, couponId);
            throw new ConflictException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        try {
            // 2. 재고 확인
            String stockStr = redisTemplate.opsForValue().get(stockKey);
            if (stockStr == null) {
                redisTemplate.opsForSet().remove(userSetKey, userId.toString());
                log.error("쿠폰 재고 캐시 없음: couponId={}", couponId);
                throw new NotFoundException(CouponErrorCode.COUPON_NOT_FOUND);
            }

            Long stock = Long.parseLong(stockStr);
            Long currentSize = redisTemplate.opsForSet().size(userSetKey);

            log.info("쿠폰 발급 시도: userId={}, couponId={}, currentSize={}, stock={}",
                userId, couponId, currentSize, stock);

            // 3. 재고 초과 검증
            if (currentSize != null && currentSize > stock) {
                redisTemplate.opsForSet().remove(userSetKey, userId.toString());
                log.warn("쿠폰 재고 부족: couponId={}, currentSize={}, stock={}",
                    couponId, currentSize, stock);
                throw new ConflictException(CouponErrorCode.COUPON_SOLD_OUT);
            }

            // 4. 발급 큐에 추가 (RPUSH - 원자적)
            CouponIssueQueueItem queueItem = CouponIssueQueueItem.create(userId, couponId);
            redisTemplate.opsForList().rightPush(queueKey, queueItem.toJson());

            log.info("쿠폰 발급 큐 추가 완료: userId={}, couponId={}", userId, couponId);

        } catch (ConflictException | NotFoundException e) {
            // 이미 롤백됨
            throw e;
        } catch (Exception e) {
            // 예상치 못한 오류 시 롤백
            redisTemplate.opsForSet().remove(userSetKey, userId.toString());
            log.error("쿠폰 발급 중 예외 발생: userId={}, couponId={}", userId, couponId, e);
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
     * 특정 쿠폰의 Redis 캐시를 초기화합니다.
     * <p>
     * - 재고 수량 (stock) 설정
     * - 기존 발급된 사용자 목록 (Set) 복원
     *
     * @param couponId 쿠폰 ID
     */
    public void initializeCouponCache(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrThrow(couponId);

        String stockKey = String.format(COUPON_STOCK_KEY, couponId);
        String userSetKey = String.format(COUPON_USER_SET_KEY, couponId);

        // 1. 재고 수량 설정
        redisTemplate.opsForValue().set(stockKey, String.valueOf(coupon.getTotalQuantity()));

        // 2. 기존 발급된 사용자 목록 복원
        List<UserCoupon> issuedUserCoupons = userCouponRepository.findByCoupon_Id(couponId);
        if (!issuedUserCoupons.isEmpty()) {
            String[] userIds = issuedUserCoupons.stream()
                .map(uc -> uc.getUserId().toString())
                .toArray(String[]::new);
            redisTemplate.opsForSet().add(userSetKey, userIds);
        }

        log.info("쿠폰 Redis 캐시 초기화: couponId={}, stock={}, issuedCount={}",
            couponId, coupon.getTotalQuantity(), issuedUserCoupons.size());
    }

    /**
     * DB의 모든 쿠폰 정보를 Redis에 일괄 초기화합니다.
     * <p>
     * 서버 시작 시 또는 수동 API 호출로 실행됩니다.
     */
    @Transactional(readOnly = true)
    public void initializeAllCouponCache() {
        log.info("전체 쿠폰 캐시 초기화 시작");

        List<Coupon> allCoupons = couponRepository.findAll();

        for (Coupon coupon : allCoupons) {
            String stockKey = String.format(COUPON_STOCK_KEY, coupon.getId());
            String userSetKey = String.format(COUPON_USER_SET_KEY, coupon.getId());

            // 1. 재고 수량 설정
            redisTemplate.opsForValue().set(stockKey, String.valueOf(coupon.getTotalQuantity()));

            // 2. 기존 발급된 사용자 목록 복원
            List<UserCoupon> issuedUserCoupons = userCouponRepository.findByCoupon_Id(coupon.getId());
            if (!issuedUserCoupons.isEmpty()) {
                String[] userIds = issuedUserCoupons.stream()
                    .map(uc -> uc.getUserId().toString())
                    .toArray(String[]::new);
                redisTemplate.opsForSet().add(userSetKey, userIds);
            }
        }

        log.info("전체 쿠폰 캐시 초기화 완료: {} 개 쿠폰", allCoupons.size());
    }

    /**
     * 쿠폰 관련 Redis 캐시를 모두 삭제합니다.
     */
    public void clearCache() {
        Set<String> keys = redisTemplate.keys("coupon:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("쿠폰 캐시 삭제 완료");
    }

    /**
     * 특정 쿠폰의 발급 큐 키를 반환합니다. (스케줄러에서 사용)
     */
    public String getQueueKey(Long couponId) {
        return String.format(COUPON_QUEUE_KEY, couponId);
    }

    /**
     * 모든 쿠폰 ID 목록을 반환합니다. (스케줄러에서 사용)
     */
    public List<Long> getAllCouponIds() {
        return couponRepository.findAll().stream()
            .map(Coupon::getId)
            .toList();
    }
}