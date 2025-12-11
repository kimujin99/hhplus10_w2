package com.example.hhplus_ecommerce.cart.model;

import com.example.hhplus_ecommerce.common.model.BaseEntity;
import com.example.hhplus_ecommerce.common.presentation.exception.BadRequestException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class CartItem extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long productId;
    private String productName;
    private Long price;
    private Integer quantity;

    public void updateQuantity(Integer newQuantity) {
        if(newQuantity <= 0) {
            throw new BadRequestException(CommonErrorCode.INVALID_QUANTITY);
        }
        this.quantity = newQuantity;
    }
}