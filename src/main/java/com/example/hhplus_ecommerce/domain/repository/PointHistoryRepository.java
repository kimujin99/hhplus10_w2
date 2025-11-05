package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.PointHistory;

import java.util.List;

public interface PointHistoryRepository {
    PointHistory findById(Long pointHistoryId);
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findByUserId(Long productId);
}
