package com.loopers.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEventMessage<T> {
    private UUID eventId;        // 멱등 처리용
    private String eventName;    // LIKE_CREATED, ORDER_PAID
    private String aggregateType;// PRODUCT, ORDER
    private Long aggregateId;    // productId / orderId
    private T payload;           // 실제 데이터
    private Instant occurredAt;

    public static <T> KafkaEventMessage<T> of(
            UUID eventId,
            String eventName,
            String aggregateType,
            Long aggregateId,
            T payload
    ) {
        return new KafkaEventMessage<>(
                eventId,
                eventName,
                aggregateType,
                aggregateId,
                payload,
                Instant.now()
        );
    }
}
