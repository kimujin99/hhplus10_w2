package com.example.hhplus_ecommerce.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "공통 API 응답")
public class ApiResponse<T> {

    private LocalDateTime timestamp;
    private String path;
    private Boolean success;
    private T data;
    private ErrorResponse error;

    public static <T> ApiResponse<T> success(String path, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .path(path)
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String path, ErrorResponse error) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .path(path)
                .success(false)
                .error(error)
                .build();
    }
}