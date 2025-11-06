package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<Long, Order> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Order> findById(Long orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }

    @Override
    public Order save(Order order) {
        if(order.getId() == null){
            order.assignId(idGenerator.getAndIncrement());
            order.onCreate();
            storage.put(order.getId(), order);
        } else {
            order.onUpdate();
            storage.put(order.getId(), order);
        }
        return order;
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .toList();
    }
}