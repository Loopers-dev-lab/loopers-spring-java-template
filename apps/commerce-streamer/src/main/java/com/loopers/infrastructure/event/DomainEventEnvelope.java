package com.loopers.infrastructure.event;

/**
 * 도메인 이벤트 봉투 - commerce-api와 동일한 구조
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
public record DomainEventEnvelope(
        String eventId,
        String eventType,
        String version,
        long occurredAtEpochMillis,
        String payloadJson
) {
}