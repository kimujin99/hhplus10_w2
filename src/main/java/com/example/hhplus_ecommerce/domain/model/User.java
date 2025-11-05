package com.example.hhplus_ecommerce.domain.model;

import lombok.Getter;

@Getter
public class User extends BaseEntity {
    private Long point;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    public User() {
        this.point = 0L;
    }

    public void usePoint(Long point) {
        if(point == null || point <= 0) {
            throw new IllegalArgumentException("사용할 포인트는 0보다 커야합니다.");
        }
        if(this.point < point) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.point -= point;
        onUpdate();
    }

    public void chargePoint(Long point) {
        if(point == null || point <= 0) {
            throw new IllegalArgumentException("충전할 포인트는 0보다 커야합니다.");
        }
        if(point % 1000 != 0) {
            throw new IllegalArgumentException("충전은 1000원 단위로 가능합니다.");
        }
        this.point += point;
        onUpdate();
    }
}
