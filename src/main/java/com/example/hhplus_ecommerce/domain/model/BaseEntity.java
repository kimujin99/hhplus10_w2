package com.example.hhplus_ecommerce.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {
    protected Long id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    public void assignId(Long id){
        this.id = id;
    }
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
