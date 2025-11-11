package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.presentation.common.response.ApiResponse;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch error: parameter={}, value={}, requiredType={}",
                ex.getName(), ex.getValue(), ex.getRequiredType());
        String message = "잘못된 형식의 파라미터입니다. "+ex.getName()+" 값은 "
                +ex.getRequiredType().getSimpleName()+" 타입이어야 합니다.";

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.INVALID_PARAMETER_TYPE, message)
        );
    }

    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingPathVariable(MissingPathVariableException ex) {
        log.warn("Missing path variable error: variable={}", ex.getVariableName());
        String message = "필수 경로 변수가 누락되었습니다: "+ex.getVariableName();

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.MISSING_PATH_VARIABLE, message)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument error: {}", ex.getMessage());

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.INVALID_ARGUMENT, ex.getMessage())
        );
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        log.warn("Business exception: errorCode={}, message={}", ex.getErrorCode(), ex.getMessage());

        return ApiResponse.error(
                ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY, message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Message not readable error: {}", ex.getMessage());

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY, "요청 본문을 읽을 수 없습니다.")
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        String message = "필수 요청 파라미터가 누락되었습니다: "+ex.getParameterName();

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.MISSING_REQUEST_PARAMETER, message)
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        String message = "지원하지 않는 HTTP 메소드입니다: "+ex.getMethod();

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, message)
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<Void> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getContentType());
        String message = "지원하지 않는 Content-Type입니다.";

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, message)
        );
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException ex) {
        log.warn("Binding error: {}", ex.getMessage());
        String message = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.BINDING_ERROR, message)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.CONSTRAINT_VIOLATION, message)
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ApiResponse.error(
                ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.")
        );
    }
}
