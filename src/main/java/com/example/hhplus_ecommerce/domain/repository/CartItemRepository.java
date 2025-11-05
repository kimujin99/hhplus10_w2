package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.CartItem;

import java.util.List;

public interface CartItemRepository {
    CartItem findById(Long id);
    CartItem save(CartItem cartItem);
    List<CartItem> findByUserId(Long userId);
}
