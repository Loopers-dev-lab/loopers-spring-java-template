package com.loopers.infrastructure.tracking;

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
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;
import com.loopers.infrastructure.tracking.client.AnalyticsClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유저 행동 추적 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 유저 행동 이벤트를 수신하여 분석 시스템으로 전송합니다.
 * 트랜잭션과 완전 분리되어 비즈니스 로직에 영향을 주지 않습니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserBehaviorEventHandler {
    private static final String CATALOG_EVENTS_TOPIC = "catalog-events";

    private final AnalyticsClient analyticsClient;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final OutboxRepository outboxRepository;

    /**
     * 유저 행동 이벤트 처리
     * <p>
     * AFTER_COMMIT + @Async로 완전한 트랜잭션 분리
     * 1. 분석 시스템으로 데이터 전송
     * 2. PRODUCT_VIEW인 경우 Kafka 이벤트용 Outbox 저장
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserBehavior(UserBehaviorEvent event) {
        // 1. 분석 시스템으로 데이터 전송
        try {
            log.debug("유저 행동 분석 데이터 전송 시작 - eventType: {}, userId: {}, targetId: {}",
                    event.eventType(), event.userId(), event.targetId());

            boolean success = analyticsClient.sendBehaviorData(event);

            if (success) {
                log.debug("유저 행동 분석 데이터 전송 성공 - eventType: {}, userId: {}",
                        event.eventType(), event.userId());
            } else {
                log.warn("유저 행동 분석 데이터 전송 실패 - eventType: {}, userId: {}",
                        event.eventType(), event.userId());
                // TODO: 실패한 이벤트를 재처리 큐에 넣거나 로컬 저장소에 백업
            }

        } catch (Exception e) {
            // 분석 시스템 전송 실패해도 비즈니스 로직에는 영향 없음
            log.error("유저 행동 분석 데이터 전송 중 예외 발생 - eventType: {}, userId: {}",
                    event.eventType(), event.userId(), e);

            // TODO: 실패한 이벤트를 재처리 큐에 넣거나 로컬 저장소에 백업
        }

        // 2. PRODUCT_VIEW인 경우 Kafka 이벤트용 Outbox 저장
        if ("PRODUCT_VIEW".equals(event.eventType())) {
            try {
                saveProductViewToOutbox(event);
            } catch (Exception e) {
                log.error("상품 조회 이벤트 Outbox 저장 실패 - userId: {}, targetId: {}",
                        event.userId(), event.targetId(), e);
            }
        }
    }

    /**
     * 상품 조회 이벤트를 Outbox에 저장
     */
    private void saveProductViewToOutbox(UserBehaviorEvent event) {
        final Long productId = event.targetId();
        final Long userId = event.userId();

        if (productId == null || userId == null) {
            log.warn("Outbox 저장 스킵 - 필수값 누락 productId={}, userId={}", productId, userId);
            return;
        }

        final ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, userId);
        final var envelope = envelopeFactory.create(EventTypes.PRODUCT_VIEW, EventVersions.V1, payload);
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

        log.info("상품 조회 이벤트 Outbox 저장 완료 - type={}, productId={}, userId={}",
                EventTypes.PRODUCT_VIEW, productId, userId);
    }
}
