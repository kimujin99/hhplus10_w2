package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(storage.get(userId));
    }

    @Override
    public User save(User user) {
        if(user.getId() == null){
            user.assignId(idGenerator.getAndIncrement());
            user.onCreate();
            storage.put(user.getId(), user);
        } else {
            user.onUpdate();
            storage.put(user.getId(), user);
        }
        return user;
    }
}
