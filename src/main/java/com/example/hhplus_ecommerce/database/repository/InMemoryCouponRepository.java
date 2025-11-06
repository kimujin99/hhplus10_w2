package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.repository.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<Long, Coupon> storage = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Coupon> findById(Long couponId) {
        throttle(300);
        return Optional.ofNullable(storage.get(couponId));
    }

    @Override
    public Coupon save(Coupon coupon) {
        if(coupon.getId() == null){
            coupon.assignId(idGenerator.getAndIncrement());
            coupon.onCreate();
            storage.put(coupon.getId(), coupon);
        } else {
            throttle(500);
            coupon.onUpdate();
            storage.put(coupon.getId(), coupon);
        }
        return coupon;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void lock(Long couponId) {
        locks.computeIfAbsent(couponId, k -> new ReentrantLock()).lock();
    }

    @Override
    public void unlock(Long couponId) {
        ReentrantLock lock = locks.get(couponId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
