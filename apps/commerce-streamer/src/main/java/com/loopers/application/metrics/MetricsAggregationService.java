package com.loopers.application.metrics;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.infrastructure.persistence.EventHandledRepository;
import com.loopers.infrastructure.persistence.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메트릭 집계 서비스
 * - Kafka 이벤트를 받아서 ProductMetrics 테이블 업데이트
 * - 멱등성 보장: event_handled 테이블로 중복 처리 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MetricsAggregationService {

    private final ProductMetricsRepository metricsRepository;
    private final EventHandledRepository eventHandledRepository;

    /**
     * 상품 좋아요 이벤트 처리
     */
    public void handleProductLiked(String eventId, ProductLikedEvent payload) {
        // 멱등성 체크
        if (eventHandledRepository.existsById(eventId)) {
            log.warn("[Metrics] Duplicate event ignored - eventId: {}, type: PRODUCT_LIKED", eventId);
            return;
        }

        // ProductMetrics 업데이트
        ProductMetrics metrics = metricsRepository.findById(payload.productId())
            .orElse(ProductMetrics.create(payload.productId()));

        metrics.incrementLikeCount();
        metricsRepository.save(metrics);

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "PRODUCT_LIKED", String.valueOf(payload.productId()))
        );

        log.info("[Metrics] Like count incremented - productId: {}, count: {}",
            payload.productId(), metrics.getLikeCount());
    }

    /**
     * 상품 좋아요 취소 이벤트 처리
     */
    public void handleProductUnliked(String eventId, ProductUnlikedEvent payload) {
        // 멱등성 체크
        if (eventHandledRepository.existsById(eventId)) {
            log.warn("[Metrics] Duplicate event ignored - eventId: {}, type: PRODUCT_UNLIKED", eventId);
            return;
        }

        // ProductMetrics 업데이트
        ProductMetrics metrics = metricsRepository.findById(payload.productId())
            .orElse(ProductMetrics.create(payload.productId()));

        metrics.decrementLikeCount();
        metricsRepository.save(metrics);

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "PRODUCT_UNLIKED", String.valueOf(payload.productId()))
        );

        log.info("[Metrics] Like count decremented - productId: {}, count: {}",
            payload.productId(), metrics.getLikeCount());
    }

    /**
     * 주문 완료 이벤트 처리 (판매량 집계)
     */
    public void handleOrderCompleted(String eventId, OrderCompletedEvent payload) {
        // 멱등성 체크
        if (eventHandledRepository.existsById(eventId)) {
            log.warn("[Metrics] Duplicate event ignored - eventId: {}, type: ORDER_COMPLETED", eventId);
            return;
        }

        // 각 주문 아이템별로 ProductMetrics 업데이트
        for (var item : payload.items()) {
            ProductMetrics metrics = metricsRepository.findById(item.productId())
                .orElse(ProductMetrics.create(item.productId()));

            // 판매 금액 = 수량 * 단가
            long totalAmount = item.quantity() * item.price().longValue();
            metrics.addSales(item.quantity(), totalAmount);
            metricsRepository.save(metrics);

            log.debug("[Metrics] Sales updated - productId: {}, quantity: {}, amount: {}",
                item.productId(), item.quantity(), totalAmount);
        }

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "ORDER_COMPLETED", payload.orderNo())
        );

        log.info("[Metrics] Sales aggregated for order: {} ({} items)",
            payload.orderNo(), payload.items().size());
    }

    /**
     * 상품 조회 이벤트 처리 (조회수 집계)
     */
    public void handleProductViewed(String eventId, ProductViewedEvent payload) {
        // 멱등성 체크
        if (eventHandledRepository.existsById(eventId)) {
            log.warn("[Metrics] Duplicate event ignored - eventId: {}, type: PRODUCT_VIEWED", eventId);
            return;
        }

        // ProductMetrics 업데이트
        ProductMetrics metrics = metricsRepository.findById(payload.productId())
            .orElse(ProductMetrics.create(payload.productId()));

        metrics.incrementViewCount();
        metricsRepository.save(metrics);

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "PRODUCT_VIEWED", String.valueOf(payload.productId()))
        );

        log.info("[Metrics] View count incremented - productId: {}, count: {}",
            payload.productId(), metrics.getViewCount());
    }
}
