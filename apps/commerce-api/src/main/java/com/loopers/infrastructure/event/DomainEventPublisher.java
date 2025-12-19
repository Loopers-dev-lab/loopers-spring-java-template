package com.loopers.infrastructure.event;

import java.util.Objects;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void publish(final String topic, final String key, final DomainEventEnvelope envelope) {
        Objects.requireNonNull(topic, "topic은 필수입니다.");
        Objects.requireNonNull(key, "key는 필수입니다.");
        Objects.requireNonNull(envelope, "envelope은 필수입니다.");
        kafkaTemplate.send(topic, key, envelope);
    }
}
