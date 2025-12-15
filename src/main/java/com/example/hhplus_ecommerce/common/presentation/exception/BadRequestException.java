package com.example.hhplus_ecommerce.common.presentation.exception;

import com.example.hhplus_ecommerce.common.presentation.errorCode.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}