package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(Long orderId);
    Order save(Order order);
    List<Order> findByUserId(Long userId);
}