package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.domain.repository.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final Map<Long, UserCoupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<UserCoupon> findById(Long userCouponId) {
        throttle(300);
        return Optional.ofNullable(storage.get(userCouponId));
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if(userCoupon.getId() == null){
            userCoupon.assignId(idGenerator.getAndIncrement());
            userCoupon.onCreate();
            storage.put(userCoupon.getId(), userCoupon);
        } else {
            throttle(500);
            userCoupon.onUpdate();
            storage.put(userCoupon.getId(), userCoupon);
        }
        return userCoupon;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId))
                .toList();
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        throttle(300);
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId)
                        && userCoupon.getCouponId().equals(couponId))
                .findFirst();
    }

    @Override
    public Optional<UserCoupon> findByOrderId(Long orderId) {
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getOrderId() != null
                        && userCoupon.getOrderId().equals(orderId))
                .findFirst();
    }
}