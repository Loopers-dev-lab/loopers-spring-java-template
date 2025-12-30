package com.loopers.domain.stock.event;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 재고 메트릭 관련 이벤트 핸들러
 * 판매량 메트릭 업데이트 및 캐시 evict 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMetricsEventHandler {

    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    public void handleProcessed(StockEvents.Processed event) {
        log.info("StockMetricsEventHandler: StockEvents.Processed 처리 - orderId: {}", event.orderId());

        if (event.orderItems() != null) {
            event.orderItems().forEach(item -> {
                productCacheService.evictProductCache(item.productId());
                productMetricsService.upsertSalesCount(item.productId(), item.quantity()); // 판매량 업데이트
            });
        }
    }

    public void handleCompensated(StockEvents.Compensated event) {
        log.info("StockMetricsEventHandler: StockEvents.Compensated 처리 - orderId: {}. Cache eviction skipped due to missing item info.", 
                event.orderId());

        // 보상 이벤트는 추가 처리 없음
    }
}

