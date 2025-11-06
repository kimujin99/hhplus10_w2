package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository {
    Optional<OrderItem> findById(Long orderItemId);
    OrderItem save(OrderItem orderItem);
    List<OrderItem> findByOrderId(Long orderId);
}