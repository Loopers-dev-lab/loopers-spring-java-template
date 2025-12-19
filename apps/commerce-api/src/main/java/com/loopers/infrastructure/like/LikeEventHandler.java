package com.loopers.infrastructure.like;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.event.EventTypes;
import com.loopers.domain.event.EventVersions;
import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxRepository;
import com.loopers.domain.like.event.LikeChangedEvent;
import com.loopers.domain.product.ProductMVService;
import com.loopers.infrastructure.event.DomainEventEnvelopeFactory;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좋아요 도메인 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 도메인 엔티티에서 발행된 좋아요 이벤트를 처리합니다.
 * Week 7 요구사항: 집계 로직의 성공/실패와 상관없이 좋아요 처리는 정상 완료되어야 함
 *
 * @author hyunjikoh
 * @since 2025. 12. 10.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeEventHandler {
    private static final String CATALOG_EVENTS_TOPIC = "catalog-events";

    private final ProductMVService productMVService;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final OutboxRepository outboxRepository;

    /**
     * 좋아요 변경 이벤트 처리
     * <p>
     * AFTER_COMMIT + @Async로 좋아요 트랜잭션과 완전 분리
     * 1. MV 테이블 집계 업데이트
     * 2. Kafka 이벤트용 Outbox 저장
     *
     * @param event 좋아요 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleLikeChanged(LikeChangedEvent event) {
        // 1. MV 테이블 집계 업데이트
        try {
            log.debug("좋아요 집계 업데이트 시작 - productId: {}, action: {}, delta: {}",
                    event.productId(), event.action(), event.countDelta());

            productMVService.updateLikeCount(event.productId(), event.countDelta());

            log.debug("좋아요 집계 업데이트 완료 - productId: {}, delta: {}",
                    event.productId(), event.countDelta());

        } catch (Exception e) {
            // 집계 업데이트 실패해도 좋아요 처리에는 영향 없음
            log.error("좋아요 집계 업데이트 실패 - productId: {}, action: {}, delta: {}",
                    event.productId(), event.action(), event.countDelta(), e);
        }

        // 2. Kafka 이벤트용 Outbox 저장
        try {
            saveToOutbox(event);
        } catch (Exception e) {
            log.error("좋아요 이벤트 Outbox 저장 실패 - productId: {}, action: {}",
                    event.productId(), event.action(), e);
        }
    }

    /**
     * 좋아요 이벤트를 Outbox에 저장
     */
    private void saveToOutbox(LikeChangedEvent event) {
        final Long productId = event.productId();
        final Long userId = event.userId();
        final String action = event.action().name(); // LIKE or UNLIKE

        if (productId == null || userId == null || action == null) {
            log.warn("Outbox 저장 스킵 - 필수값 누락 productId={}, userId={}, action={}",
                    productId, userId, action);
            return;
        }

        final LikeActionPayloadV1 payload = new LikeActionPayloadV1(productId, userId, action);
        final var envelope = envelopeFactory.create(EventTypes.LIKE_ACTION, EventVersions.V1, payload);
        final String partitionKey = String.valueOf(productId);

        // Outbox에 저장 (READY 상태)
        OutboxEventEntity outboxEvent = OutboxEventEntity.ready(
                envelope.eventId(),
                CATALOG_EVENTS_TOPIC,
                partitionKey,
                envelope.eventType(),
                envelope.version(),
                envelope.occurredAtEpochMillis(),
                envelope.payloadJson()
        );

        outboxRepository.save(outboxEvent);

        log.info("좋아요 이벤트 Outbox 저장 완료 - type={}, productId={}, userId={}, action={}",
                EventTypes.LIKE_ACTION, productId, userId, action);
    }
}
