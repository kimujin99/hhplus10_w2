package com.example.hhplus_ecommerce.infrastructure.repository;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.infrastructure.dto.ProductScore;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findById(Long productId);

    default Product findByIdOrThrow(Long productId) {
        return findById(productId)
                .orElseThrow(() -> new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    default Product findByIdWithLockOrThrow(Long productId) {
        return findByIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    Product save(Product product);
    List<Product> findAll();

    @Query(value = """
        SELECT p.*
        FROM product p
        ORDER BY (p.view_count + (p.original_stock_quantity - p.stock_quantity) * 2) DESC
        LIMIT 5
    """, nativeQuery = true)
    List<Product> findPopularProduct();

    @Query("""
        SELECT new com.example.hhplus_ecommerce.infrastructure.dto.ProductScore(p.id,
            p.viewCount + (p.originalStockQuantity - p.stockQuantity) * 2)
        FROM Product p
    """)
    List<ProductScore> findAllProductScores();

    @Modifying
    @Query(value = "UPDATE product SET view_count = view_count + 1 WHERE id = :id", nativeQuery = true)
    void incrementViewCount(@Param("id") Long id);
}
