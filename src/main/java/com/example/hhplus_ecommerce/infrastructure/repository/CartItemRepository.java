package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findById(Long cartItemId);
    CartItem save(CartItem cartItem);
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    void deleteById(Long cartItemId);
    void deleteByUserId(Long userId);
}
