package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.CartItem;
import com.example.hhplus_ecommerce.domain.repository.CartItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCartItemRepository implements CartItemRepository {
    private final Map<Long, CartItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<CartItem> findById(Long cartItemId) {
        return Optional.ofNullable(storage.get(cartItemId));
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

    @Override
    public Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return storage.values().stream()
                .filter(cartItem -> cartItem.getUserId().equals(userId) && cartItem.getProductId().equals(productId))
                .findFirst();
    }

    @Override
    public void delete(Long cartItemId) {
        storage.remove(cartItemId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        storage.values().removeIf(cartItem -> cartItem.getUserId().equals(userId));
    }
}
