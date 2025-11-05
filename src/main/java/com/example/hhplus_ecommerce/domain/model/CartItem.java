package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItem extends BaseEntity {
    private Long userId;
    private Long productId;
    private Integer quantity;

    public void addQuantity() {
        this.quantity++;
        onUpdate();
    }

    public void subQuantity() {
        if(this.quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY, "상품 수량은 하나 이상이어야 합니다.");
        }
        this.quantity--;
        onUpdate();
    }
}