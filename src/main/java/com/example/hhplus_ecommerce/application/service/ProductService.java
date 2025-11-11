package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.repository.ProductRepository;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        return ProductResponse.fromList(products);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.incrementViewCount();
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductStockResponse getProductStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductStockResponse.from(product);
    }

    public List<PopularProductResponse> getPopularProducts() {
        List<Product> products = productRepository.findPopularProduct();
        return PopularProductResponse.fromList(products);
    }
}
