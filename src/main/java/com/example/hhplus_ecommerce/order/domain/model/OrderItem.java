package com.example.hhplus_ecommerce.order.domain.model;

import com.example.hhplus_ecommerce.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class OrderItem extends BaseEntity {
    @Column(nullable = false)
    private Long orderId;
    @Column(nullable = false)
    private Long productId;
    private Integer quantity;
    private String productName;
    private Long price;

    public Long getSubTotal() {
        return this.price * this.quantity;
    }
}