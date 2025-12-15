package com.example.hhplus_ecommerce.user.application;

import com.example.hhplus_ecommerce.user.domain.PointHistory;
import com.example.hhplus_ecommerce.user.domain.User;
import com.example.hhplus_ecommerce.user.infrastructure.PointHistoryRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.user.presentation.dto.UserDto.PointHistoryResponse;
import com.example.hhplus_ecommerce.user.presentation.dto.UserDto.PointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointResponse getPoint(Long userId) {
        User user = userRepository.findByIdOrThrow(userId);
        return PointResponse.from(user);
    }

    public List<PointHistoryResponse> getPointHistory(Long userId) {
        userRepository.findByIdOrThrow(userId);
        List<PointHistory> pointHistories = pointHistoryRepository.findByUserId(userId);
        return PointHistoryResponse.fromList(pointHistories);
    }
}