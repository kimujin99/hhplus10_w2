package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.ProductService;
import com.example.hhplus_ecommerce.presentation.common.ApiResponse;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponse.success(productService.getProducts());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable("productId") Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    @GetMapping("/{productId}/stock")
    public ApiResponse<ProductStockResponse> getProductStock(@PathVariable("productId") Long productId) {
        return ApiResponse.success(productService.getProductStock(productId));
    }

    @GetMapping("/popular")
    public ApiResponse<List<PopularProductResponse>> getPopularProducts() {
        return ApiResponse.success(productService.getPopularProducts());
    }
}
