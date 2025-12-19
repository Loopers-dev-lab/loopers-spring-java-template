package com.loopers.domain.product.event;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Product 캐시 관련 이벤트 핸들러
 * 캐시 evict 및 조회수 집계 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheEventHandler {

    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    public void handleCreated(ProductEvents.Created event) {
        log.info("ProductCacheEventHandler: ProductEvents.Created 처리 - productId: {}", event.productId());

        // Redis 캐시 Evict (Write-Around 전략)
        productCacheService.evictProductCache(event.productId());
    }

    public void handleUpdated(ProductEvents.Updated event) {
        log.info("ProductCacheEventHandler: ProductEvents.Updated 처리 - productId: {}", event.productId());

        // Redis 캐시 Evict (Write-Around 전략)
        productCacheService.evictProductCache(event.productId());
    }

    public void handleDeleted(ProductEvents.Deleted event) {
        log.info("ProductCacheEventHandler: ProductEvents.Deleted 처리 - productId: {}", event.productId());

        // Redis 캐시 Evict (Write-Around 전략)
        productCacheService.evictProductCache(event.productId());
    }

    public void handleViewed(ProductEvents.Viewed event) {
        log.info("ProductCacheEventHandler: ProductEvents.Viewed 처리 - productId: {}", event.productId());

        // 조회 수 집계
        productMetricsService.upsertViewCount(event.productId());
    }
}

