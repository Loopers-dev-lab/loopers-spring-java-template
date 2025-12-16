package com.loopers.infrastructure.event;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
public record DomainEventEnvelope(
        String eventId,
        String eventType,
        String version,
        long occurredAtEpochMillis,
        String payloadJson
) {
}
