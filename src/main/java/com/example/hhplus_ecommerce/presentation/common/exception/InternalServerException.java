package com.example.hhplus_ecommerce.presentation.common.exception;

import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseException {

    public InternalServerException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}