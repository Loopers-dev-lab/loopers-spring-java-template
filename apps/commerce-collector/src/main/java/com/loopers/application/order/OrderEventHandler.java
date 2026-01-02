package com.loopers.application.order;

import com.loopers.application.product.cache.ProductCacheService;
import com.loopers.application.eventhandled.EventHandledFacade;
import com.loopers.application.metrics.ProductMetricsFacade;
import com.loopers.domain.stock.StockThresholdChecker;
import com.loopers.infrastructure.client.ProductApiGateway;
import com.loopers.infrastructure.client.dto.ProductDetailExternalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final EventHandledFacade eventHandledFacade;
    private final ProductMetricsFacade productMetricsFacade;
    private final ProductApiGateway productApiGateway;
    private final StockThresholdChecker stockThresholdChecker;
    private final ProductCacheService productCacheService;

    @Transactional
    public void handleOrderCreated(String eventId, Long productId, int quantity) {
        // 1. 멱등성 체크
        if (eventHandledFacade.isAlreadyHandled(eventId)) {
            log.info("이미 처리된 주문 이벤트 - eventId: {}", eventId);
            return;
        }

        // 2. 주문 집계 증가
        productMetricsFacade.incrementOrderCount(productId, quantity);
        log.info("주문 집계 증가 - productId: {}, quantity: {}", productId, quantity);

        // 3. 상품 재고 확인
        int currentStock = getCurrentStock(productId);

        // 4. 재고 임계값 체크
        if (stockThresholdChecker.isBelowThreshold(currentStock)) {
            // 5. 캐시 무효화
            productCacheService.evictProductCache(productId);
            log.info("재고 임계값 도달로 캐시 무효화 - productId: {}, 현재 재고: {}",
                    productId, currentStock);
        }

        // 6. 이벤트 처리 완료 기록
        eventHandledFacade.markAsHandled(
                eventId, "OrderCreated", "ORDER", productId.toString()
        );

        log.info("주문 이벤트 처리 완료 - eventId: {}, productId: {}, 현재 재고: {}",
                eventId, productId, currentStock);
    }

    /**
     * commerce-api에서 현재 재고 조회
     * CircuitBreaker와 Retry는 ProductApiGateway에서 처리
     */
    private int getCurrentStock(Long productId) {
        ProductDetailExternalDto.ProductDetailResponse product = productApiGateway.getProductDetail(productId);
        return product.getStockQuantity();
    }
}
