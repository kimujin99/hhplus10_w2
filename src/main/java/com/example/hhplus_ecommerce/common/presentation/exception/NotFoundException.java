package com.example.hhplus_ecommerce.common.presentation.exception;

import com.example.hhplus_ecommerce.common.presentation.errorCode.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}