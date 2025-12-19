package com.loopers.domain.like.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.event.EventTypes;
import com.loopers.domain.event.EventVersions;
import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxRepository;
import com.loopers.infrastructure.event.DomainEventEnvelopeFactory;
import com.loopers.infrastructure.event.DomainEventPublisher;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좋아요 도메인 이벤트를 Kafka로 즉시 발송하는 핸들러
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
public class LikeKafkaEventHandler {
    private static final String CATALOG_EVENTS_TOPIC = "catalog-events";

    private final DomainEventEnvelopeFactory envelopeFactory;
    private final DomainEventPublisher domainEventPublisher;
    private final OutboxRepository outboxRepository;

    /**
     * 좋아요 변경 도메인 이벤트를 Kafka로 즉시 발송
     * 실패 시 Outbox에 저장하여 나중에 재시도
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeChanged(final LikeChangedEvent event) {
        final Long productId = event.productId();
        final Long userId = event.userId();
        final String action = event.action().name(); // LIKE or UNLIKE

        if (productId == null || userId == null || action == null) {
            log.warn("이벤트 발송 스킵 - 필수값 누락 productId={}, userId={}, action={}",
                    productId, userId, action);
            return;
        }

        final LikeActionPayloadV1 payload = new LikeActionPayloadV1(productId, userId, action);
        final var envelope = envelopeFactory.create(EventTypes.LIKE_ACTION, EventVersions.V1, payload);
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

            log.info("이벤트 즉시 발송 성공 - type={}, productId={}, userId={}, action={}",
                    EventTypes.LIKE_ACTION, productId, userId, action);

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

            log.warn("이벤트 즉시 발송 실패, Outbox에 저장 - type={}, productId={}, userId={}, action={}, error={}",
                    EventTypes.LIKE_ACTION, productId, userId, action, e.getMessage());
        }
    }
}
