package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.OrderItem;
import com.example.hhplus_ecommerce.domain.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {
    private final Map<Long, OrderItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public OrderItem findById(Long id) {
        return storage.get(id);
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        if(orderItem.getId() == null){
            orderItem.assignId(idGenerator.getAndIncrement());
            orderItem.onCreate();
            storage.put(orderItem.getId(), orderItem);
        } else {
            orderItem.onUpdate();
            storage.put(orderItem.getId(), orderItem);
        }
        return orderItem;
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return storage.values().stream()
                .filter(orderItem -> orderItem.getOrderId().equals(orderId))
                .toList();
    }
}