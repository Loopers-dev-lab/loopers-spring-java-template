package com.loopers.domain.like.event;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 좋아요 관련 이벤트 핸들러
 * 좋아요 메트릭 업데이트 및 캐시 evict 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    public void handleProductLikeSaved(LikeEvents.ProductLikeSaved event) {
        log.info("LikeEventHandler: ProductLikeSaved 처리 - productId: {}", event.productId());

        // 좋아요 수 증가 (ProductMetricsService를 통해, 멱등성 보장)
        productMetricsService.upsertLikeCount(event.productId(), 1L, event.getOccurredAt());
        // Redis 캐시 업데이트 (Write-Through)
        productCacheService.evictProductCache(event.productId());
    }

    public void handleProductLikeDeleted(LikeEvents.ProductLikeDeleted event) {
        log.info("LikeEventHandler: ProductLikeDeleted 처리 - productId: {}", event.productId());

        // 좋아요 수 감소 (ProductMetricsService를 통해, 멱등성 보장)
        productMetricsService.upsertLikeCount(event.productId(), -1L, event.getOccurredAt());
        // Redis 캐시 업데이트 (Write-Through)
        productCacheService.evictProductCache(event.productId());
    }

    public void handleLikeCountChanged(LikeEvents.LikeCountChanged event) {
        log.info("LikeEventHandler: LikeCountChanged 처리 - productId: {}, delta: {}", 
                event.productId(), event.delta());

        // ProductMetricsService를 통해 좋아요 수 집계 (멱등성 보장)
        productMetricsService.upsertLikeCount(event.productId(), event.delta(), event.getOccurredAt());
        // Redis 캐시 업데이트 (Write-Through)
        productCacheService.evictProductCache(event.productId());
    }
}

