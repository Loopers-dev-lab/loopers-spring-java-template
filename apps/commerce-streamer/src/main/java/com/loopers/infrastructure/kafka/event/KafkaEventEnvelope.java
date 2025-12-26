package com.loopers.infrastructure.kafka.event;

import java.time.ZonedDateTime;

/**
 * Kafka 이벤트 Envelope
 * - Kafka로부터 수신한 이벤트의 메타데이터 포함
 * - 멱등성 체크 및 순서 보장을 위한 정보 포함
 *
 * @param <T> Application Event 타입 (OrderPlacedEvent, ProductLikedEvent 등)
 */
public record KafkaEventEnvelope<T>(
    /**
     * 이벤트 ID (Outbox Event의 ID)
     * - Consumer에서 멱등성 체크에 사용
     */
    String eventId,

    /**
     * 이벤트 타입
     * - 예: ORDER_PLACED, PRODUCT_LIKED, PAYMENT_COMPLETED
     */
    String eventType,

    /**
     * Partition Key
     * - 디버깅 및 로깅용
     */
    String partitionKey,

    /**
     * 실제 이벤트 Payload
     * - Application Event 객체 (OrderPlacedEvent, ProductLikedEvent 등)
     */
    T payload,

    /**
     * 이벤트 발생 시각
     */
    ZonedDateTime occurredAt
) {
}
