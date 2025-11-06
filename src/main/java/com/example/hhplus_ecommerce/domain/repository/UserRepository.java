package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);
    User save(User user);
}
