package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.PopularProductResponse;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.ProductResponse;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.ProductStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        return ProductResponse.fromList(products);
    }

    @Transactional
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findByIdOrThrow(productId);
        productRepository.incrementViewCount(productId);
        return ProductResponse.from(product);
    }

    public ProductStockResponse getProductStock(Long productId) {
        Product product = productRepository.findByIdOrThrow(productId);
        return ProductStockResponse.from(product);
    }

    public List<PopularProductResponse> getPopularProducts() {
        List<Product> products = productRepository.findPopularProduct();
        return PopularProductResponse.fromList(products);
    }
}
