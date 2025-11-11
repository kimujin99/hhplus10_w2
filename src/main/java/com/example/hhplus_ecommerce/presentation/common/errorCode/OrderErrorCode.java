package com.example.hhplus_ecommerce.presentation.common.errorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "주문 상태가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}