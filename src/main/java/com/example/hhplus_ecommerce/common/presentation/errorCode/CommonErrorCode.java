package com.example.hhplus_ecommerce.common.presentation.errorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "잘못된 파라미터 타입입니다."),
    MISSING_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "필수 경로 변수가 누락되었습니다."),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 인자입니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "잘못된 요청 본문입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    BINDING_ERROR(HttpStatus.BAD_REQUEST, "바인딩 오류가 발생했습니다."),
    CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "제약 조건 위반입니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "상품 수량은 하나 이상이어야 합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "동시 요청으로 인해 처리에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}