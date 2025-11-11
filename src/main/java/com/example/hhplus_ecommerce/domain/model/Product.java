package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Product extends BaseEntity {
    private String productName;
    private String description;
    private Long price;
    private Integer originalStockQuantity;
    private Integer stockQuantity;
    private Integer viewCount;

    // TODO: 인메모리 구현용. JPA 전환 시 제거
    @Builder
    public Product(String productName, String description, Long price, Integer originalStockQuantity, Integer stockQuantity) {
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.originalStockQuantity = originalStockQuantity;
        this.stockQuantity = stockQuantity;
        this.viewCount = 0;
    }

    public void addStockQuantity(Integer stockQuantity) {
        this.stockQuantity += stockQuantity;
    }

    public void subStockQuantity(Integer stockQuantity) {
        if(this.stockQuantity < stockQuantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stockQuantity -= stockQuantity;
    }


    // TODO: 실제 DB 사용시 제거. 인메모리 인기상품 집계 구현용.
    public Integer getPurchaseCount() {
        return this.originalStockQuantity - this.stockQuantity;
    }
    public Integer getPopularityScore() {
        double salesRatio = 0.0;
        int total = this.originalStockQuantity;
        if (total > 0) {
            salesRatio = (double) getPurchaseCount() / total;
        }
        return (int) (viewCount + salesRatio * 100 * 2);
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}