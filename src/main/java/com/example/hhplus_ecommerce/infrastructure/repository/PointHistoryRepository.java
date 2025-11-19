package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Optional<PointHistory> findById(Long pointHistoryId);
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findByUserId(Long productId);
}
