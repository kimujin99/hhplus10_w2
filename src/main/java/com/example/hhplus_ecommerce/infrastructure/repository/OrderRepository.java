package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findById(Long orderId);
    Order save(Order order);
    List<Order> findByUserId(Long userId);
}