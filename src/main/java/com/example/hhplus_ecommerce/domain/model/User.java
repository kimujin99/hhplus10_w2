package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BadRequestException;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.PointErrorCode;
import lombok.Getter;

@Getter
public class User extends BaseEntity {
    private Long point;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    public User() {
        this.point = 0L;
    }

    public void usePoint(Long point) {
        if(point <= 0) {
            throw new BadRequestException(PointErrorCode.INVALID_POINT_AMOUNT);
        }
        if(this.point < point) {
            throw new ConflictException(PointErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= point;
    }

    public void chargePoint(Long point) {
        if(point <= 0) {
            throw new BadRequestException(PointErrorCode.INVALID_CHARGE_AMOUNT);
        }
        if(point % 1000 != 0) {
            throw new BadRequestException(PointErrorCode.INVALID_CHARGE_AMOUNT, "충전은 1000원 단위로 가능합니다.");
        }
        this.point += point;
    }
}
