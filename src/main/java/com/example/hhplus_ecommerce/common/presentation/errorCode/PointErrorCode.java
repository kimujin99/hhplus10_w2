package com.example.hhplus_ecommerce.common.presentation.errorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "포인트가 부족합니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "사용할 포인트는 0보다 커야합니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "충전할 포인트는 0보다 커야합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}