package com.loopers.application.product;

import com.loopers.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceWithViewCount {

    private final ProductService productService;
    private final ProductCacheService productCacheService;

    /**
     * 상품 조회 + 조회수 증가 (RedisTemplate 활용)
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
