package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.PointHistory;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.PointHistoryRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.errorCode.PointErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.BadRequestException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.ChargePointRequest;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.PointHistoryResponse;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.PointResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserPointService userPointService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("포인트 조회 성공")
    void getPoint_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().point(0L).build();
        user.chargePoint(10000L);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);

        // when
        PointResponse result = userService.getPoint(userId);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.point()).isEqualTo(10000L)
        );

        verify(userRepository).findByIdOrThrow(userId);
    }

    @Test
    @DisplayName("포인트 조회 실패 - 사용자 없음")
    void getPoint_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findByIdOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userService.getPoint(userId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
    }

    @Test
    @DisplayName("포인트 이력 조회 성공")
    void getPointHistory_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().point(0L).build();
        PointHistory history1 = PointHistory.builder()
                .userId(userId)
                .transactionType(PointHistory.TransactionType.CHARGE)
                .amount(10000L)
                .balanceAfter(10000L)
                .build();
        PointHistory history2 = PointHistory.builder()
                .userId(userId)
                .transactionType(PointHistory.TransactionType.USE)
                .amount(3000L)
                .balanceAfter(7000L)
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(pointHistoryRepository.findByUserId(userId)).willReturn(List.of(history1, history2));

        // when
        List<PointHistoryResponse> result = userService.getPointHistory(userId);

        // then
        assertThat(result).hasSize(2);
        verify(userRepository).findByIdOrThrow(userId);
        verify(pointHistoryRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("포인트 이력 조회 실패 - 사용자 없음")
    void getPointHistory_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findByIdOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userService.getPointHistory(userId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(pointHistoryRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_Success() {
        // given
        Long userId = 1L;
        ChargePointRequest request = new ChargePointRequest(5000L);
        PointResponse expectedResponse = new PointResponse(userId, 5000L);

        given(userPointService.chargePoint(userId, request)).willReturn(expectedResponse);

        // when
        PointResponse result = userPointService.chargePoint(userId, request);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.point()).isEqualTo(5000L)
        );

        verify(userPointService).chargePoint(userId, request);
    }

    @Test
    @DisplayName("포인트 충전 실패 - 사용자 없음")
    void chargePoint_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        ChargePointRequest request = new ChargePointRequest(5000L);
        given(userPointService.chargePoint(userId, request))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userPointService.chargePoint(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userPointService).chargePoint(userId, request);
    }

    @Test
    @DisplayName("포인트 충전 실패 - 잘못된 금액")
    void chargePoint_Fail_InvalidAmount() {
        // given
        Long userId = 1L;
        ChargePointRequest request = new ChargePointRequest(500L); // 1000원 단위가 아님

        given(userPointService.chargePoint(userId, request))
                .willThrow(new BadRequestException(PointErrorCode.INVALID_CHARGE_AMOUNT));

        // when & then
        assertThatThrownBy(() -> userPointService.chargePoint(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CHARGE_AMOUNT);
        verify(userPointService).chargePoint(userId, request);
    }
}