package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findById(Long productId);
    Product save(Product product);
    List<Product> findAll();

    @Query("""
        SELECT p
        FROM Product p
        ORDER BY (p.viewCount + ((p.originalStockQuantity - p.stockQuantity) * 1.0 / p.originalStockQuantity) * 100 * 2) DESC
        LIMIT 5
    """)
    List<Product> findPopularProduct();
}
