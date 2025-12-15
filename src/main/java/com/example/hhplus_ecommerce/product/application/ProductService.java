package com.example.hhplus_ecommerce.product.application;

import com.example.hhplus_ecommerce.product.domain.Product;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.product.presentation.dto.ProductDto.PopularProductResponse;
import com.example.hhplus_ecommerce.product.presentation.dto.ProductDto.ProductResponse;
import com.example.hhplus_ecommerce.product.presentation.dto.ProductDto.ProductStockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final PopularProductCacheService popularProductCacheService;

    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        return ProductResponse.fromList(products);
    }

    @Transactional
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findByIdOrThrow(productId);
        productRepository.incrementViewCount(productId);

        // Redis 캐시에 조회수 점수 증가 (비동기, 실패해도 조회에 영향 없음)
        try {
            popularProductCacheService.incrementViewScore(productId);
        } catch (Exception e) {
            // Redis 업데이트 실패는 로그만 남기고 무시
            log.error("조회수 점수 업데이트 실패: productId={}, error={}", productId, e.getMessage());
        }

        return ProductResponse.from(product);
    }

    public ProductStockResponse getProductStock(Long productId) {
        Product product = productRepository.findByIdOrThrow(productId);
        return ProductStockResponse.from(product);
    }

    /**
     * 인기 상품 목록을 조회합니다.
     * <p>
     * Redis 캐시에서 상위 5개 상품을 조회합니다.
     * 캐시가 비어있으면 DB에서 직접 조회합니다 (Fallback).
     *
     * @return 인기 상품 목록 (최대 5개)
     */
    public List<PopularProductResponse> getPopularProducts() {
        try {
            // Redis 캐시에서 상위 5개 상품 ID 조회
            List<Long> topProductIds = popularProductCacheService.getTopProductIds(5);

            if (topProductIds.isEmpty()) {
                log.warn("Redis 캐시가 비어있습니다. DB에서 직접 조회합니다.");
                return getPopularProductsFromDB();
            }

            // ID로 Product 엔티티 조회
            List<Product> products = productRepository.findAllById(topProductIds);

            // Redis 순서를 유지하며 정렬
            List<Product> sortedProducts = topProductIds.stream()
                    .map(id -> products.stream()
                            .filter(p -> p.getId().equals(id))
                            .findFirst()
                            .orElse(null))
                    .filter(p -> p != null)  // null 제거 (삭제된 상품)
                    .toList();

            return PopularProductResponse.fromList(sortedProducts);

        } catch (Exception e) {
            // Redis 장애 시 DB Fallback
            log.error("Redis 캐시 조회 실패. DB에서 조회합니다. error={}", e.getMessage());
            return getPopularProductsFromDB();
        }
    }

    /**
     * DB에서 직접 인기 상품을 조회합니다.
     * <p>
     * Redis 캐시 장애 시 Fallback으로 사용됩니다.
     *
     * @return 인기 상품 목록
     */
    private List<PopularProductResponse> getPopularProductsFromDB() {
        List<Product> products = productRepository.findPopularProduct();
        return PopularProductResponse.fromList(products);
    }
}
