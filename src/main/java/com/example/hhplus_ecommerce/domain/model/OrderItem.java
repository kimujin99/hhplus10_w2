package com.example.hhplus_ecommerce.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItem extends BaseEntity {
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private String productName;
    private Long price;

    public Long getSubTotal() {
        return this.price * this.quantity;
    }
}