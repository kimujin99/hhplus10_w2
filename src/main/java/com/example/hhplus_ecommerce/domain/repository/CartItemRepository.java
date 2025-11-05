package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.CartItem;

import java.util.List;

public interface CartItemRepository {
    CartItem findById(Long cartItemId);
    CartItem save(CartItem cartItem);
    List<CartItem> findByUserId(Long userId);
    CartItem findByUserIdAndProductId(Long userId, Long productId);
    void delete(Long cartItemId);
}
