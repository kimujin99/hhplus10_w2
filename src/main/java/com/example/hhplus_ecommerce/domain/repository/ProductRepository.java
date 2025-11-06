package com.example.hhplus_ecommerce.domain.repository;

import com.example.hhplus_ecommerce.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long productId);
    Product save(Product product);
    List<Product> findAll();
    List<Product> findPopularProduct();
}
