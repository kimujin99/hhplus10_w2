package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BadRequestException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CommonErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItem extends BaseEntity {
    private Long userId;
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