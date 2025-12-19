package com.loopers.domain.tracking;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.event.EventTypes;
import com.loopers.domain.event.EventVersions;
import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxRepository;
import com.loopers.domain.tracking.event.UserBehaviorEvent;
import com.loopers.infrastructure.event.DomainEventEnvelopeFactory;
import com.loopers.infrastructure.event.DomainEventPublisher;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 조회 행동 추적 이벤트를 Kafka로 즉시 발송하는 핸들러
 * <p>
 * Event-Driven + Outbox Fallback 패턴:
 * 1. 트랜잭션 커밋 후 즉시 Kafka 발송 시도
 * 2. 성공 시 Outbox에 SENT 상태로 기록 (모니터링용)
 * 3. 실패 시 Outbox에 FAILED 상태로 기록 (재시도용)
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductViewKafkaEventHandler {
    private static final String CATALOG_EVENTS_TOPIC = "catalog-events";

    private final DomainEventEnvelopeFactory envelopeFactory;
    private final DomainEventPublisher domainEventPublisher;
    private final OutboxRepository outboxRepository;

    /**
     * 상품 조회 행동 추적 이벤트를 Kafka로 즉시 발송
     * PRODUCT_VIEW 타입의 UserBehaviorEvent만 처리
     * 실패 시 Outbox에 저장하여 나중에 재시도
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductViewBehavior(final UserBehaviorEvent event) {
        // PRODUCT_VIEW 타입만 처리
        if (!"PRODUCT_VIEW".equals(event.eventType())) {
            return;
        }

        final Long productId = event.targetId();
        final Long userId = event.userId();

        if (productId == null || userId == null) {
            log.warn("이벤트 발송 스킵 - 필수값 누락 productId={}, userId={}", productId, userId);
            return;
        }

        final ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, userId);
        final var envelope = envelopeFactory.create(EventTypes.PRODUCT_VIEW, EventVersions.V1, payload);
        final String partitionKey = String.valueOf(productId);

        try {
            // 1. 즉시 Kafka 발송 시도
            domainEventPublisher.publish(CATALOG_EVENTS_TOPIC, partitionKey, envelope);

            // 2. 성공 시 Outbox에 SENT 상태로 기록 (모니터링용)
            OutboxEventEntity sent = OutboxEventEntity.ready(
                    envelope.eventId(),
                    CATALOG_EVENTS_TOPIC,
                    partitionKey,
                    envelope.eventType(),
                    envelope.version(),
                    envelope.occurredAtEpochMillis(),
                    envelope.payloadJson()
            );
            sent.markSent(); // SENT 상태로 변경
            outboxRepository.save(sent);

            log.info("상품 조회 이벤트 즉시 발송 성공 - type={}, productId={}, userId={}",
                    EventTypes.PRODUCT_VIEW, productId, userId);

        } catch (Exception e) {
            // 3. 실패 시 Outbox에 FAILED 상태로 기록 (재시도용)
            OutboxEventEntity failed = OutboxEventEntity.ready(
                    envelope.eventId(),
                    CATALOG_EVENTS_TOPIC,
                    partitionKey,
                    envelope.eventType(),
                    envelope.version(),
                    envelope.occurredAtEpochMillis(),
                    envelope.payloadJson()
            );
            failed.markFailed(); // FAILED 상태로 변경
            outboxRepository.save(failed);

            log.warn("상품 조회 이벤트 즉시 발송 실패, Outbox에 저장 - type={}, productId={}, userId={}, error={}",
                    EventTypes.PRODUCT_VIEW, productId, userId, e.getMessage());
        }
    }
}
