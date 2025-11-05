package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.PointHistory;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.repository.PointHistoryRepository;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointResponse getPoint(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        return PointResponse.from(user);
    }

    public List<PointHistoryResponse> getPointHistory(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        List<PointHistory> pointHistories = pointHistoryRepository.findByUserId(userId);
        return PointHistoryResponse.fromList(pointHistories);
    }

    public PointResponse chargePoint(Long userId, ChargePointRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

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