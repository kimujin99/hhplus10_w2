package com.example.hhplus_ecommerce.order.infrastructure;

import com.example.hhplus_ecommerce.order.domain.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findById(Long orderItemId);
    OrderItem save(OrderItem orderItem);
    List<OrderItem> findByOrderId(Long orderId);
}