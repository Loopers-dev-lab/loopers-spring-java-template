package com.loopers.application.metrics;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.infrastructure.cache.ProductRankingCache;
import com.loopers.infrastructure.cache.ProductRankingCache.OrderItemScore;
import com.loopers.infrastructure.persistence.EventHandledRepository;
import com.loopers.infrastructure.persistence.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

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
    private final ProductRankingCache productRankingCache;

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

        // 랭킹 ZSET 점수 추가
        productRankingCache.addLikeScore(payload.productId());

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "PRODUCT_LIKED", String.valueOf(payload.productId()))
        );

        log.info("[Metrics] Like count incremented - productId: {}, count: {}",
            payload.productId(), metrics.getLikeCount());
    }

    /**
     * 상품 좋아요 취소 이벤트 처리
     * - 랭킹 점수도 함께 감소 (일관성 유지)
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

        // 랭킹 ZSET 점수 감소 (좋아요 취소 반영)
        productRankingCache.subtractLikeScore(payload.productId());

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "PRODUCT_UNLIKED", String.valueOf(payload.productId()))
        );

        log.info("[Metrics] Like count decremented - productId: {}, count: {}",
            payload.productId(), metrics.getLikeCount());
    }

    /**
     * 주문 완료 이벤트 처리 (판매량 집계)
     * - Redis Pipeline을 사용하여 여러 아이템의 랭킹 점수를 한 번에 업데이트
     */
    public void handleOrderCompleted(String eventId, OrderCompletedEvent payload) {
        // 멱등성 체크
        if (eventHandledRepository.existsById(eventId)) {
            log.warn("[Metrics] Duplicate event ignored - eventId: {}, type: ORDER_COMPLETED", eventId);
            return;
        }

        // 랭킹 점수 배치 처리용 목록
        var orderItemScores = new ArrayList<OrderItemScore>();

        // 각 주문 아이템별로 ProductMetrics 업데이트
        for (var item : payload.items()) {
            ProductMetrics metrics = metricsRepository.findById(item.productId())
                .orElse(ProductMetrics.create(item.productId()));

            // 판매 금액 = 수량 * 단가
            long totalAmount = item.quantity() * item.price().longValue();
            metrics.addSales(item.quantity(), totalAmount);
            metricsRepository.save(metrics);

            // 랭킹 점수 배치 목록에 추가
            orderItemScores.add(new OrderItemScore(
                item.productId(),
                item.price().longValue(),
                item.quantity()
            ));

            log.debug("[Metrics] Sales updated - productId: {}, quantity: {}, amount: {}",
                item.productId(), item.quantity(), totalAmount);
        }

        // 랭킹 ZSET 점수 배치 추가 (Pipeline 사용)
        productRankingCache.addOrderScoresBatch(orderItemScores);

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

        // 랭킹 ZSET 점수 추가
        productRankingCache.addViewScore(payload.productId());

        // 처리 완료 기록
        eventHandledRepository.save(
            EventHandled.create(eventId, "PRODUCT_VIEWED", String.valueOf(payload.productId()))
        );

        log.info("[Metrics] View count incremented - productId: {}, count: {}",
            payload.productId(), metrics.getViewCount());
    }
}
