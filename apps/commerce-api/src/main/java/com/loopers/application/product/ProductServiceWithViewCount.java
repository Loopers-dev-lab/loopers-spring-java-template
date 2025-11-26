package com.loopers.application.product;

import com.loopers.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProductService 확장 - RedisTemplate 활용 예시
 *
 * 목적: 조회수 실시간 카운팅 기능 추가
 *
 * 사용 방법 (선택적):
 * 1. 기본 조회: ProductService.getProduct() - Spring Cache만 사용
 * 2. 조회수 포함: ProductServiceWithViewCount.getProductWithViewCount() - RedisTemplate 추가
 *
 * 현재 프로젝트:
 * - 필수 아님 (Spring Cache로 충분)
 * - 블로그 포스팅용 예시 코드
 * - Nice-to-have 기능
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceWithViewCount {

    private final ProductService productService;
    private final ProductCacheService productCacheService;

    /**
     * 상품 조회 + 조회수 증가 (RedisTemplate 활용)
     *
     * 장점:
     * - 조회수 실시간 반영
     * - DB 부하 없음 (Redis만 업데이트)
     * - 원자성 보장 (Redis INCR)
     *
     * 사용 예시:
     * - 인기 상품 순위
     * - 실시간 조회수 표시
     */
    public ProductWithViewCount getProductWithViewCount(Long id) {
        // 1. 상품 조회 (Spring Cache)
        Product product = productService.getProduct(id);

        // 2. 조회수 증가 (RedisTemplate)
        Long viewCount = productCacheService.incrementViewCount(id);

        return new ProductWithViewCount(product, viewCount);
    }

    /**
     * 조회수만 조회 (캐시 통계용)
     */
    public Long getViewCount(Long productId) {
        return productCacheService.getViewCount(productId);
    }

    /**
     * 상품 정보 + 조회수 DTO
     */
    public record ProductWithViewCount(
        Product product,
        Long viewCount
    ) {
    }
}
