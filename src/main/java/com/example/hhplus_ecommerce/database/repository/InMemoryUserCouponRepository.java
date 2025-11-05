package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.domain.repository.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final Map<Long, UserCoupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserCoupon findById(Long userCouponId) {
        return storage.get(userCouponId);
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if(userCoupon.getId() == null){
            userCoupon.assignId(idGenerator.getAndIncrement());
            userCoupon.onCreate();
            storage.put(userCoupon.getId(), userCoupon);
        } else {
            userCoupon.onUpdate();
            storage.put(userCoupon.getId(), userCoupon);
        }
        return userCoupon;
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId))
                .toList();
    }

    @Override
    public UserCoupon findByUserIdAndCouponId(Long userId, Long couponId) {
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId)
                        && userCoupon.getCouponId().equals(couponId))
                .findFirst()
                .orElse(null);
    }
}