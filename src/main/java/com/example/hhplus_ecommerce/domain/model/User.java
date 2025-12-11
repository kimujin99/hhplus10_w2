package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BadRequestException;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.PointErrorCode;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class User extends BaseEntity {
    @ColumnDefault("0L") @Builder.Default
    private Long point = 0L;

    private static int CHARGE_AMOUNT_POLICY = 1000;

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
        if(point % CHARGE_AMOUNT_POLICY != 0) {
            throw new BadRequestException(PointErrorCode.INVALID_CHARGE_AMOUNT, "충전은 "+CHARGE_AMOUNT_POLICY+"원 단위로 가능합니다.");
        }
        this.point += point;
    }
}
