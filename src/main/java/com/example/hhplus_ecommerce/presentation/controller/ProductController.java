package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.ProductService;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<ProductStockResponse> getProductStock(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(productService.getProductStock(productId));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PopularProductResponse>> getPopularProducts() {
        return ResponseEntity.ok(productService.getPopularProducts());
    }
}
