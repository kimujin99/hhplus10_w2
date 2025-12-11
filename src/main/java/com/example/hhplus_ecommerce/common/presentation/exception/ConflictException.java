package com.example.hhplus_ecommerce.common.presentation.exception;

import com.example.hhplus_ecommerce.common.presentation.errorCode.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}