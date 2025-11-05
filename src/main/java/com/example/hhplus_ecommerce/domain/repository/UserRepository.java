package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.User;

public interface UserRepository {
    User findById(Long userId);
    User save(User user);
}
