package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.repository.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<Long, Coupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Coupon findById(Long couponId) {
        return storage.get(couponId);
    }

    @Override
    public Coupon save(Coupon coupon) {
        if(coupon.getId() == null){
            coupon.assignId(idGenerator.getAndIncrement());
            coupon.onCreate();
            storage.put(coupon.getId(), coupon);
        } else {
            coupon.onUpdate();
            storage.put(coupon.getId(), coupon);
        }
        return coupon;
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(storage.values());
    }
}
