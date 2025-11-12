package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.presentation.common.errorCode.OrderErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findById(Long orderId);

    default Order findByIdOrThrow(Long orderId) {
        return findById(orderId)
                .orElseThrow(() -> new NotFoundException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    Order save(Order order);
    List<Order> findByUserId(Long userId);
}