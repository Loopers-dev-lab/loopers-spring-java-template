package com.loopers.application.event;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loopers.application.event.dto.EventProcessingResult.CatalogEventResult;
import com.loopers.application.event.dto.EventProcessingResult.OrderEventResult;
import com.loopers.application.metrics.MetricsService;
import com.loopers.cache.dto.CachePayloads.RankingScore;
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
 * 이벤트 처리 Application Facade
 * <p>
 * Kafka Consumer로부터 받은 이벤트를 처리하는 Application 계층 Facade입니다.
 * 여러 도메인 서비스를 조합하여 이벤트를 처리합니다.
 * <p>
 * 책임:
 * - 이벤트 역직렬화 및 유효성 검증
 * - 과거 이벤트 필터링
 * - 멱등성 체크 위임
 * - 메트릭 처리 위임
 * - 랭킹 점수 생성 위임
 * <p>
 * 의존관계:
 * - MetricsFacade (Application) - 메트릭 처리
 * - RankingService (Domain) - 랭킹 점수 생성
 * - EventDeserializer (Infrastructure) - 이벤트 역직렬화
 *
 * @author hyunjikoh
 * @since 2025.12.23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingFacade {

    // Application Layer 의존성
    private final MetricsService metricsService;

    // Domain Layer 의존성
    private final RankingService rankingService;

    // Infrastructure Layer 의존성
    private final EventDeserializer eventDeserializer;

    // 설정값
    private static final long OLD_EVENT_THRESHOLD_MS = 60 * 60 * 1000; // 1시간

    /**
     * 카탈로그 이벤트 처리 (조회, 좋아요, 재고 소진)
     *
     * @param eventValue 이벤트 원본 데이터
     * @return 처리 결과 (랭킹 점수 포함)
     */
    public CatalogEventResult processCatalogEvent(Object eventValue) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(eventValue);

        if (!isValidEnvelope(envelope)) {
            log.warn("Invalid event envelope: {}", eventValue);
            return CatalogEventResult.notProcessed();
        }

        // 과거 이벤트 필터링
        if (isOldEvent(envelope.occurredAtEpochMillis())) {
            log.debug("Ignoring old event: eventId={}, occurredAt={}",
                    envelope.eventId(), envelope.occurredAtEpochMillis());
            metricsService.tryMarkHandled(envelope.eventId());
            return CatalogEventResult.notProcessed();
        }

        // 멱등성 체크
        if (!metricsService.tryMarkHandled(envelope.eventId())) {
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

        if (!isValidEnvelope(envelope)) {
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
        if (!metricsService.tryMarkHandled(envelope.eventId())) {
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

    // ========== Private Methods - 이벤트 타입별 처리 ==========

    private CatalogEventResult processProductView(DomainEventEnvelope envelope) {
        final ProductViewPayloadV1 payload = eventDeserializer.deserializeProductView(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("Invalid ProductView payload: {}", envelope.payloadJson());
            return CatalogEventResult.notProcessed();
        }

        metricsService.incrementView(payload.productId(), envelope.occurredAtEpochMillis());
        log.debug("Processed PRODUCT_VIEW for productId: {}", payload.productId());

        RankingScore rankingScore = rankingService.generateRankingScore(envelope);
        return CatalogEventResult.processed(rankingScore);
    }

    private CatalogEventResult processLikeAction(DomainEventEnvelope envelope) {
        final LikeActionPayloadV1 payload = eventDeserializer.deserializeLikeAction(envelope.payloadJson());
        if (payload == null || payload.productId() == null || payload.action() == null) {
            log.warn("Invalid LikeAction payload: {}", envelope.payloadJson());
            return CatalogEventResult.notProcessed();
        }

        final int delta = "LIKE".equals(payload.action()) ? 1 : -1;
        metricsService.applyLikeDelta(payload.productId(), delta, envelope.occurredAtEpochMillis());
        log.debug("Processed LIKE_ACTION for productId: {}, action: {}", payload.productId(), payload.action());

        RankingScore rankingScore = rankingService.generateRankingScore(envelope);
        return CatalogEventResult.processed(rankingScore);
    }

    private CatalogEventResult processStockDepleted(DomainEventEnvelope envelope) {
        final StockDepletedPayloadV1 payload = eventDeserializer.deserializeStockDepleted(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("Invalid StockDepleted payload: {}", envelope.payloadJson());
            return CatalogEventResult.notProcessed();
        }

        metricsService.handleStockDepleted(
                payload.productId(),
                payload.brandId(),
                payload.remainingStock(),
                envelope.occurredAtEpochMillis()
        );

        log.info("Processed STOCK_DEPLETED - productId: {}, brandId: {}, productName: {}, remainingStock: {}",
                payload.productId(), payload.brandId(), payload.productName(), payload.remainingStock());

        return CatalogEventResult.notProcessed();
    }

    private OrderEventResult processPaymentSuccess(DomainEventEnvelope envelope) {
        final PaymentSuccessPayloadV1 payload = eventDeserializer.deserializePaymentSuccess(envelope.payloadJson());
        if (payload == null) {
            log.warn("Invalid PaymentSuccess payload: {}", envelope.payloadJson());
            return OrderEventResult.notProcessed();
        }

        if (payload.productId() == null || payload.quantity() == null || payload.quantity() <= 0) {
            log.warn("Invalid PaymentSuccess payload - missing required fields: productId={}, quantity={}",
                    payload.productId(), payload.quantity());
            return OrderEventResult.notProcessed();
        }

        metricsService.addSales(payload.productId(), payload.quantity(), payload.totalPrice(), envelope.occurredAtEpochMillis());

        log.debug("Processed PAYMENT_SUCCESS - orderId: {}, productId: {}, quantity: {}, totalPrice: {}",
                payload.orderId(), payload.productId(), payload.quantity(), payload.totalPrice());

        RankingScore rankingScore = rankingService.generateRankingScore(envelope);
        return OrderEventResult.processed(rankingScore);
    }

    // ========== Private Methods - 유틸리티 ==========

    private boolean isValidEnvelope(DomainEventEnvelope envelope) {
        return envelope != null && envelope.eventId() != null;
    }

    private boolean isOldEvent(long occurredAtEpochMillis) {
        long eventAge = System.currentTimeMillis() - occurredAtEpochMillis;
        return eventAge > OLD_EVENT_THRESHOLD_MS;
    }
}
