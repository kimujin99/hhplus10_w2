package com.example.hhplus_ecommerce.presentation.dto;

import com.example.hhplus_ecommerce.domain.model.Product;

import java.util.List;

public class ProductDto {

    public record ProductResponse(
            Long productId,
            String productName,
            String description,
            Long price,
            Integer stockQuantity,
            Integer viewCount
    ){
        public static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(),
                    product.getProductName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStockQuantity(),
                    product.getViewCount()
            );
        }

        public static List<ProductResponse> fromList(List<Product> products) {
            return products.stream()
                    .map(ProductResponse::from)
                    .toList();
        }
    }

    public record ProductStockResponse(
            Long productId,
            Integer stockQuantity
    ){
        public static ProductStockResponse from(Product product) {
            return new ProductStockResponse(
                    product.getId(),
                    product.getStockQuantity()
            );
        }
    }

    public record PopularProductResponse(
            Long productId,
            String productName,
            Integer viewCount
    ){
        public static PopularProductResponse from(Product product) {
            return new PopularProductResponse(
                    product.getId(),
                    product.getProductName(),
                    product.getViewCount()
            );
        }

        public static List<PopularProductResponse> fromList(List<Product> products) {
            return products.stream()
                    .map(PopularProductResponse::from)
                    .toList();
        }
    }
}
