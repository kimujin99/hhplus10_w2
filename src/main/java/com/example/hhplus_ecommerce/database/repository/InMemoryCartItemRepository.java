package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.CartItem;
import com.example.hhplus_ecommerce.domain.repository.CartItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCartItemRepository implements CartItemRepository {
    private final Map<Long, CartItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public CartItem findById(Long cartItemId) {
        return storage.get(cartItemId);
    }

    @Override
    public CartItem save(CartItem cartItem) {
        if(cartItem.getId() == null){
            cartItem.assignId(idGenerator.getAndIncrement());
            cartItem.onCreate();
            storage.put(cartItem.getId(), cartItem);
        } else {
            cartItem.onUpdate();
            storage.put(cartItem.getId(), cartItem);
        }
        return cartItem;
    }

    @Override
    public List<CartItem> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(cartItem -> cartItem.getUserId().equals(userId))
                .toList();
    }
}
