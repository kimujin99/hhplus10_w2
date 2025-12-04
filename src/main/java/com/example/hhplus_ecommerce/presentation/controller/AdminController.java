package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.PopularProductCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 API 컨트롤러
 * <p>
 * 시스템 관리 및 캐시 관리 기능을 제공합니다.
 */
@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PopularProductCacheService popularProductCacheService;

    /**
     * 인기 상품 캐시를 초기화합니다.
     * <p>
     * DB의 모든 상품 데이터를 기반으로 Redis 캐시를 재구성합니다.
     * 서버 최초 실행 시 또는 캐시 재구축이 필요할 때 사용합니다.
     *
     * @return 성공 메시지
     */
    @Operation(summary = "인기 상품 캐시 초기화", description = "DB 데이터를 기반으로 Redis 캐시를 재구성합니다")
    @PostMapping("/cache/popular-products/init")
    public ResponseEntity<String> initPopularProductsCache() {
        popularProductCacheService.initializeCache();
        return ResponseEntity.ok("인기 상품 캐시 초기화가 완료되었습니다.");
    }

    /**
     * 인기 상품 캐시를 삭제합니다.
     *
     * @return 성공 메시지
     */
    @Operation(summary = "인기 상품 캐시 삭제", description = "Redis의 인기 상품 캐시를 삭제합니다")
    @DeleteMapping("/cache/popular-products")
    public ResponseEntity<String> clearPopularProductsCache() {
        popularProductCacheService.clearCache();
        return ResponseEntity.ok("인기 상품 캐시가 삭제되었습니다.");
    }
}