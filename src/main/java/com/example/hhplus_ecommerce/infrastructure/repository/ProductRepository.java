package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findById(Long productId);

    default Product findByIdOrThrow(Long productId) {
        return findById(productId)
                .orElseThrow(() -> new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    Product save(Product product);
    List<Product> findAll();

    @Query(value = """
        SELECT p.*
        FROM product p
        ORDER BY (p.view_count + ((p.original_stock_quantity - p.stock_quantity) * 1.0 / p.original_stock_quantity) * 100 * 2) DESC
        LIMIT 5
    """, nativeQuery = true)
    List<Product> findPopularProduct();
}
