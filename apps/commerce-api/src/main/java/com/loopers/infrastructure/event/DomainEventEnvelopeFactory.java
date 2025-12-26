package com.loopers.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
@Component
@RequiredArgsConstructor
public class DomainEventEnvelopeFactory {
    private final ObjectMapper objectMapper;

    public DomainEventEnvelope create(final String eventType, final String version, final Object payload) {
        final String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 payload 직렬화에 실패했습니다.", e);
        }

        return new DomainEventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                version,
                Instant.now().toEpochMilli(),
                payloadJson
        );
    }

}
