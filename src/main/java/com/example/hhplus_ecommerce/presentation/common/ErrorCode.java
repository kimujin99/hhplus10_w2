package com.example.hhplus_ecommerce.presentation.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_PARAMETER_TYPE("잘못된 파라미터 타입입니다."),
    MISSING_PATH_VARIABLE("필수 경로 변수가 누락되었습니다."),
    INVALID_ARGUMENT("잘못된 인자입니다."),
    INVALID_REQUEST_BODY("잘못된 요청 본문입니다."),
    MISSING_REQUEST_PARAMETER("필수 요청 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED("허용되지 않은 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE("지원하지 않는 미디어 타입입니다."),
    BINDING_ERROR("바인딩 오류가 발생했습니다."),
    CONSTRAINT_VIOLATION("제약 조건 위반입니다."),

    PRODUCT_NOT_FOUND("존재하지 않는 상품입니다."),
    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    ORDER_NOT_FOUND("존재하지 않는 주문입니다."),
    CART_ITEM_NOT_FOUND("장바구니 항목을 찾을 수 없습니다."),
    COUPON_NOT_FOUND("존재하지 않는 쿠폰입니다."),

    INSUFFICIENT_STOCK("재고가 부족합니다."),
    INSUFFICIENT_POINT("포인트가 부족합니다."),
    INVALID_POINT_AMOUNT("사용할 포인트는 0보다 커야합니다."),
    INVALID_CHARGE_AMOUNT("충전할 포인트는 0보다 커야합니다."),
    INVALID_QUANTITY("상품 수량은 하나 이상이어야 합니다."),
    INVALID_ORDER_STATUS("주문 상태가 올바르지 않습니다."),

    COUPON_EXPIRED("쿠폰이 만료되었습니다."),
    COUPON_SOLD_OUT("쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_USED("이미 사용된 쿠폰입니다."),
    COUPON_ALREADY_ISSUED("이미 발급받은 쿠폰입니다."),
    COUPON_NOT_USED("사용되지 않은 쿠폰입니다."),

    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;
}
