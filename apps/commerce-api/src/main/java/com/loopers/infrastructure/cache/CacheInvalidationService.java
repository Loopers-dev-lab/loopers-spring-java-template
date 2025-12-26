package com.loopers.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheInvalidationService {

    private final ProductDetailCache productDetailCache;

    /**
     * Invalidate caches when a product is liked or unliked
     *
     * @param productId The product that was liked/unliked
     * @param brandId The brand of the product
     */
    public void invalidateOnLikeChange(Long productId, Long brandId) {
        log.info("[CacheInvalidation] Invalidating caches for productId={}, brandId={}", productId, brandId);

        // Invalidate product detail cache
        productDetailCache.delete(productId);

        // Note: Product list cache는 TTL(60초)에 의존하여 자동 무효화

        log.info("[CacheInvalidation] Cache invalidation completed for productId={}, brandId={}", productId, brandId);
    }

    /**
     * Invalidate cache when product info is updated (name, price, description, etc.)
     */
    public void invalidateOnProductUpdate(Long productId) {
        log.info("[CacheInvalidation] Invalidating cache for product update, productId={}", productId);
        productDetailCache.delete(productId);
    }

    /**
     * Invalidate cache when product stock is depleted
     */
    public void invalidateOnStockDepletion(Long productId) {
        log.info("[CacheInvalidation] Invalidating cache for stock depletion, productId={}", productId);
        productDetailCache.delete(productId);
        // Note: Product list cache는 TTL(60초)에 의존하여 자동 무효화
    }
}
