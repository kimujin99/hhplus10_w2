package com.example.hhplus_ecommerce.common.presentation.errorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CouponErrorCode implements ErrorCode {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.CONFLICT, "쿠폰이 만료되었습니다."),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
    COUPON_NOT_USED(HttpStatus.CONFLICT, "사용되지 않은 쿠폰입니다."),
    COUPON_ISSUE_CONFLICT(HttpStatus.CONFLICT, "쿠폰 발급 중 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}