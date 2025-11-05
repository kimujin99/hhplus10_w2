package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.Order;

import java.util.List;

public interface OrderRepository {
    Order findById(Long orderId);
    Order save(Order order);
    List<Order> findByUserId(Long userId);
}