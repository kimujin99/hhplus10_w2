package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Product extends BaseEntity {
    private String productName;
    private String description;
    private Long price;
    private Integer stockQuantity;
    private Integer viewCount;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    @Builder
    public Product(String productName, String description, Long price, Integer stockQuantity) {
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.viewCount = 0;
    }

    public void addStockQuantity(Integer stockQuantity) {
        this.stockQuantity += stockQuantity;
        onUpdate();
    }

    public void subStockQuantity(Integer stockQuantity) {
        if(this.stockQuantity < stockQuantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "재고가 부족합니다.");
        }
        this.stockQuantity -= stockQuantity;
        onUpdate();
    }

    public void incrementViewCount() {
        this.viewCount++;
        onUpdate();
    }
}