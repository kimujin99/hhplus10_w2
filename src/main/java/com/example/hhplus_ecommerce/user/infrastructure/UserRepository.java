package com.example.hhplus_ecommerce.user.infrastructure;

import com.example.hhplus_ecommerce.user.domain.User;
import com.example.hhplus_ecommerce.common.presentation.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long userId);

    default User findByIdOrThrow(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    default User findByIdWithLockOrThrow(Long userId) {
        return findByIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    User save(User user);
}
