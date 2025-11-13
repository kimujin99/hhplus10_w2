package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.PointHistory;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.PointHistoryRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.ChargePointRequest;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.PointHistoryResponse;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.PointResponse;
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

    @Transactional
    public PointResponse chargePoint(Long userId, ChargePointRequest request) {
        User user = userRepository.findByIdOrThrow(userId);

        user.chargePoint(request.amount());
        User savedUser = userRepository.save(user);

        PointHistory pointHistory = PointHistory.builder()
                .userId(userId)
                .transactionType(PointHistory.TransactionType.CHARGE)
                .amount(request.amount())
                .balanceAfter(savedUser.getPoint())
                .build();
        pointHistoryRepository.save(pointHistory);

        return PointResponse.from(savedUser);
    }
}