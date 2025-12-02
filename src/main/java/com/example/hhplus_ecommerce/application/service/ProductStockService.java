package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.infrastructure.lock.DistributedLock;
import com.example.hhplus_ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 상품 재고 관리 서비스
 * <p>
 * 상품 재고의 차감 및 복구를 담당합니다.
 * 분산 락을 사용하여 다중 서버 환경에서의 재고 동시성을 제어합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductRepository productRepository;

    /**
     * 상품 재고를 차감합니다.
     * <p>
     * 동시성 제어:
     * - 분산 락(Redisson RLock)을 사용하여 상품별 동시성 제어
     * - 다중 서버 환경에서 같은 상품의 재고 차감 순차 처리
     * - 새로운 트랜잭션에서 실행되어 독립적으로 커밋/롤백
     *
     * @param productId 재고를 차감할 상품 ID
     * @param quantity 차감할 수량
     * @throws NotFoundException 상품을 찾을 수 없는 경우
     * @throws ConflictException 재고가 부족한 경우
     */
    @DistributedLock(
        key = "product:#{#productId}:stock",
        waitTime = 10L,
        leaseTime = 5L,
        timeUnit = TimeUnit.SECONDS
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdOrThrow(productId);
        product.subStockQuantity(quantity);
        productRepository.save(product);

        log.info("재고 차감 성공: productId={}, quantity={}, remainingStock={}",
            productId, quantity, product.getStockQuantity());
    }

    /**
     * 상품 재고를 복구합니다.
     * <p>
     * Saga 패턴의 보상 트랜잭션으로 사용됩니다.
     * 결제 실패 시 차감된 재고를 원복합니다.
     *
     * @param productId 재고를 복구할 상품 ID
     * @param quantity 복구할 수량
     */
    @DistributedLock(
        key = "product:#{#productId}:stock",
        waitTime = 10L,
        leaseTime = 5L,
        timeUnit = TimeUnit.SECONDS
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdOrThrow(productId);
        product.addStockQuantity(quantity);
        productRepository.save(product);

        log.info("재고 복구 성공: productId={}, quantity={}, currentStock={}",
            productId, quantity, product.getStockQuantity());
    }
}