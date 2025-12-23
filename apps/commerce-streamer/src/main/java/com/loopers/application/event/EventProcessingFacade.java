package com.loopers.application.event;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loopers.cache.dto.CachePayloads.RankingScore;
import com.loopers.domain.metrics.MetricsService;
import com.loopers.domain.ranking.RankingService;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.EventDeserializer;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;
import com.loopers.infrastructure.event.payloads.StockDepletedPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트 처리 파사드
 * <p>
 * 여러 도메인 서비스(메트릭, 랭킹 등)를 조합하여 이벤트를 처리하는 응용 계층 서비스
 *
 * @author hyunjikoh
 * @since 2025.12.23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingFacade {

    private final MetricsService metricsService;
    private final RankingService rankingService;
    private final EventDeserializer eventDeserializer;

    /**
     * 카탈로그 이벤트 처리 결과
     */
    public record CatalogEventResult(
            boolean processed,
            RankingScore rankingScore
    ) {
        public static CatalogEventResult notProcessed() {
            return new CatalogEventResult(false, null);
        }

        public static CatalogEventResult processed(RankingScore rankingScore) {
            return new CatalogEventResult(true, rankingScore);
        }
    }

    /**
     * 주문 이벤트 처리 결과
     */
    public record OrderEventResult(
            boolean processed,
            RankingScore rankingScore
    ) {
        public static OrderEventResult notProcessed() {
            return new OrderEventResult(false, null);
        }

        public static OrderEventResult processed(RankingScore rankingScore) {
            return new OrderEventResult(true, rankingScore);
        }
    }

    /**
     * 카탈로그 이벤트 처리 (조회, 좋아요, 재고 소진)
     *
     * @param eventValue 이벤트 원본 데이터
     * @return 처리 결과 (랭킹 점수 포함)
     */
    public CatalogEventResult processCatalogEvent(Object eventValue) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(eventValue);
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Invalid event envelope: {}", eventValue);
            return CatalogEventResult.notProcessed();
        }

        // 과거 이벤트 필터링 (1시간 이상 된 이벤트는 무시)
        if (isOldEvent(envelope.occurredAtEpochMillis())) {
            log.debug("Ignoring old event: eventId={}, occurredAt={}",
                    envelope.eventId(), envelope.occurredAtEpochMillis());
            metricsService.tryMarkHandled(envelope.eventId());
            return CatalogEventResult.notProcessed();
        }

        // 멱등성 체크 - 이미 처리된 이벤트는 무시
        final boolean isFirstTime = metricsService.tryMarkHandled(envelope.eventId());
        if (!isFirstTime) {
            log.debug("Event already processed: {}", envelope.eventId());
            return CatalogEventResult.notProcessed();
        }

        // 이벤트 타입별 처리
        return switch (envelope.eventType()) {
            case "PRODUCT_VIEW" -> processProductView(envelope);
            case "LIKE_ACTION" -> processLikeAction(envelope);
            case "STOCK_DEPLETED" -> processStockDepleted(envelope);
            default -> {
                log.debug("Unhandled catalog event type: {}", envelope.eventType());
                yield CatalogEventResult.notProcessed();
            }
        };
    }

    /**
     * 주문 이벤트 처리 (결제 성공)
     *
     * @param eventValue 이벤트 원본 데이터
     * @return 처리 결과 (랭킹 점수 포함)
     */
    public OrderEventResult processOrderEvent(Object eventValue) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(eventValue);
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Invalid event envelope: {}", eventValue);
            return OrderEventResult.notProcessed();
        }

        // 과거 이벤트 필터링
        if (isOldEvent(envelope.occurredAtEpochMillis())) {
            log.debug("Ignoring old event: eventId={}, occurredAt={}",
                    envelope.eventId(), envelope.occurredAtEpochMillis());
            metricsService.tryMarkHandled(envelope.eventId());
            return OrderEventResult.notProcessed();
        }

        // 멱등성 체크
        final boolean isFirstTime = metricsService.tryMarkHandled(envelope.eventId());
        if (!isFirstTime) {
            log.debug("Event already processed: {}", envelope.eventId());
            return OrderEventResult.notProcessed();
        }

        // PAYMENT_SUCCESS 이벤트만 처리
        if ("PAYMENT_SUCCESS".equals(envelope.eventType())) {
            return processPaymentSuccess(envelope);
        } else {
            log.debug("Unhandled order event type: {}", envelope.eventType());
            return OrderEventResult.notProcessed();
        }
    }

    /**
     * 랭킹 점수 배치 업데이트
     *
     * @param rankingScores 랭킹 점수 리스트
     * @param targetDate    대상 날짜
     */
    public void updateRankingScores(List<RankingScore> rankingScores, LocalDate targetDate) {
        if (rankingScores == null || rankingScores.isEmpty()) {
            return;
        }

        try {
            rankingService.updateRankingScoresBatch(rankingScores, targetDate);
            log.debug("랭킹 점수 배치 업데이트 완료: {} scores", rankingScores.size());
        } catch (Exception e) {
            log.error("랭킹 점수 배치 업데이트 실패: date={}, scores={}", targetDate, rankingScores.size(), e);
        }
    }

    // ========== Private Methods ==========

    private CatalogEventResult processProductView(DomainEventEnvelope envelope) {
        final ProductViewPayloadV1 payload = eventDeserializer.deserializeProductView(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("Invalid ProductView payload: {}", envelope.payloadJson());
            return CatalogEventResult.notProcessed();
        }

        // 메트릭 처리
        metricsService.incrementView(payload.productId(), envelope.occurredAtEpochMillis());
        log.debug("Processed PRODUCT_VIEW for productId: {}", payload.productId());

        // 랭킹 점수 생성
        RankingScore rankingScore = rankingService.generateRankingScore(envelope);
        return CatalogEventResult.processed(rankingScore);
    }

    private CatalogEventResult processLikeAction(DomainEventEnvelope envelope) {
        final LikeActionPayloadV1 payload = eventDeserializer.deserializeLikeAction(envelope.payloadJson());
        if (payload == null || payload.productId() == null || payload.action() == null) {
            log.warn("Invalid LikeAction payload: {}", envelope.payloadJson());
            return CatalogEventResult.notProcessed();
        }

        // 메트릭 처리
        final int delta = "LIKE".equals(payload.action()) ? 1 : -1;
        metricsService.applyLikeDelta(payload.productId(), delta, envelope.occurredAtEpochMillis());
        log.debug("Processed LIKE_ACTION for productId: {}, action: {}", payload.productId(), payload.action());

        // 랭킹 점수 생성 (좋아요만 반영)
        RankingScore rankingScore = rankingService.generateRankingScore(envelope);
        return CatalogEventResult.processed(rankingScore);
    }

    private CatalogEventResult processStockDepleted(DomainEventEnvelope envelope) {
        final StockDepletedPayloadV1 payload = eventDeserializer.deserializeStockDepleted(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("Invalid StockDepleted payload: {}", envelope.payloadJson());
            return CatalogEventResult.notProcessed();
        }

        // 재고 소진 이벤트 처리
        metricsService.handleStockDepleted(
                payload.productId(),
                payload.brandId(),
                payload.remainingStock(),
                envelope.occurredAtEpochMillis()
        );

        log.info("Processed STOCK_DEPLETED - productId: {}, brandId: {}, productName: {}, remainingStock: {}",
                payload.productId(), payload.brandId(), payload.productName(), payload.remainingStock());

        // 재고 소진은 랭킹에 영향 없음
        return CatalogEventResult.notProcessed();
    }

    private OrderEventResult processPaymentSuccess(DomainEventEnvelope envelope) {
        final PaymentSuccessPayloadV1 payload = eventDeserializer.deserializePaymentSuccess(envelope.payloadJson());
        if (payload == null) {
            log.warn("Invalid PaymentSuccess payload: {}", envelope.payloadJson());
            return OrderEventResult.notProcessed();
        }

        // 상품별 개별 이벤트 처리
        if (payload.productId() != null && payload.quantity() != null && payload.quantity() > 0) {
            // 메트릭 처리
            metricsService.addSales(payload.productId(), payload.quantity(), envelope.occurredAtEpochMillis());

            log.debug(
                    "Processed PAYMENT_SUCCESS - orderId: {}, orderNumber: {}, userId: {}, productId: {}, quantity: {}, unitPrice: {}, totalPrice: {}",
                    payload.orderId(), payload.orderNumber(), payload.userId(),
                    payload.productId(), payload.quantity(), payload.unitPrice(), payload.totalPrice());

            // 랭킹 점수 생성
            RankingScore rankingScore = rankingService.generateRankingScore(envelope);
            return OrderEventResult.processed(rankingScore);
        } else {
            log.warn("Invalid PaymentSuccess payload - missing required fields: productId={}, quantity={}",
                    payload.productId(), payload.quantity());
            return OrderEventResult.notProcessed();
        }
    }

    /**
     * 과거 이벤트인지 확인 (1시간 이상 된 이벤트는 과거 이벤트로 간주)
     */
    private boolean isOldEvent(long occurredAtEpochMillis) {
        long currentTime = System.currentTimeMillis();
        long eventAge = currentTime - occurredAtEpochMillis;
        long oneHourInMillis = 60 * 60 * 1000; // 1시간

        return eventAge > oneHourInMillis;
    }
}
