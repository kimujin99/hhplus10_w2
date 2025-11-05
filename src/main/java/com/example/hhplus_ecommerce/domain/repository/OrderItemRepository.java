package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.OrderItem;

import java.util.List;

public interface OrderItemRepository {
    OrderItem findById(Long orderItemId);
    OrderItem save(OrderItem orderItem);
    List<OrderItem> findByOrderId(Long orderId);
}