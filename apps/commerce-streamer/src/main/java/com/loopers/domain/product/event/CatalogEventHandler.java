package com.loopers.domain.product.event;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 카탈로그 관련 이벤트 핸들러
 * 캐시 evict 및 좋아요 메트릭 업데이트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventHandler {

    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    public void handleUpdated(ProductEvents.Updated event) {
        log.info("CatalogEventHandler: ProductEvents.Updated 처리 - productId: {}", event.productId());

        productCacheService.evictProductCache(event.productId());
    }

    public void handleDeleted(ProductEvents.Deleted event) {
        log.info("CatalogEventHandler: ProductEvents.Deleted 처리 - productId: {}", event.productId());

        productCacheService.evictProductCache(event.productId());
    }

    public void handleLikeCount(ProductEvents.LikeCount event) {
        log.info("CatalogEventHandler: ProductEvents.LikeCount 처리 - productId: {} (delta: {})", 
                event.productId(), event.delta());

        productMetricsService.upsertLikeCount(event.productId(), event.delta());
    }
}

