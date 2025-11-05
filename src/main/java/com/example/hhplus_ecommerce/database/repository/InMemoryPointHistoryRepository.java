package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.PointHistory;
import com.example.hhplus_ecommerce.domain.repository.PointHistoryRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPointHistoryRepository implements PointHistoryRepository {
    private final Map<Long, PointHistory> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public PointHistory findById(Long id) {
        return storage.get(id);
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {
        if(pointHistory.getId() == null){
            pointHistory.assignId(idGenerator.getAndIncrement());
            pointHistory.onCreate();
            storage.put(pointHistory.getId(), pointHistory);
        } else {
            pointHistory.onUpdate();
            storage.put(pointHistory.getId(), pointHistory);
        }
        return pointHistory;
    }

    @Override
    public List<PointHistory> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(pointHistory -> pointHistory.getUserId().equals(userId))
                .toList();
    }
}
