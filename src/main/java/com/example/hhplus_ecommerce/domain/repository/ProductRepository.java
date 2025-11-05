package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.Product;

import java.util.List;

public interface ProductRepository {
    Product findById(Long productId);
    Product save(Product product);
    List<Product> findAll();
}
