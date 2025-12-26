package com.loopers.domain.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DomainEventEnvelop<T> {

    private UUID eventId;           // 멱등 처리 키
    private String eventType;       // LIKE_CREATED, LIKE_CANCELED, ORDER_PAID, PRODUCT_VIEWED

    private String aggregateType;   // PRODUCT, ORDER
    private Long aggregateId;       // 파티션 키

    private T payload;
    private Instant occurredAt;

    public static  <T> DomainEventEnvelop<T> of(
            String eventType,
            String aggregateType,
            Long aggregateId,
            T payload
    ) {
        return new DomainEventEnvelop<>(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                payload,
                Instant.now()
        );
    }
}
