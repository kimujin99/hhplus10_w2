package com.example.hhplus_ecommerce.common.presentation.errorCode;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getHttpStatus();
    String getMessage();
}