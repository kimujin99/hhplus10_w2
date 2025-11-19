package com.example.hhplus_ecommerce.presentation.common.exception;

import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
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