package com.example.hhplus_ecommerce.presentation.common.response;

import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private ErrorCode code;
    private String message;

    public static ErrorResponse of(ErrorCode code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .build();
    }
}