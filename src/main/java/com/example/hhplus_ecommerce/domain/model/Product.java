package com.example.hhplus_ecommerce.domain.model;

import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class Product extends BaseEntity {
    private String productName;
    private String description;
    private Long price;
    private Integer originalStockQuantity;
    private Integer stockQuantity;
    @ColumnDefault("0")
    @Builder.Default
    private Integer viewCount = 0;

    public void addStockQuantity(Integer stockQuantity) {
        this.stockQuantity += stockQuantity;
    }

    public void subStockQuantity(Integer stockQuantity) {
        if(this.stockQuantity < stockQuantity) {
            throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        this.stockQuantity -= stockQuantity;
    }

    public Integer getPurchaseCount() {
        return this.originalStockQuantity - this.stockQuantity;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}