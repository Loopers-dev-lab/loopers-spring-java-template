package com.loopers.domain.common.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "domain_event")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DomainEvent {

    @Id
    private UUID id;                        // 멱등 키

    private String topic;                   // "order-events", "catalog-events"
    private String eventType;               // LIKE_CREATED, LIKE_CANCELED, ORDER_PAID, PRODUCT_VIEWED

    private String aggregateType;           // PRODUCT, ORDER
    private Long aggregateId;               // 파티션 키


    @Lob
    private String payload;                 // Json

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private int retryCount;                 // send 실패 시 재시도 카운트
    private ZonedDateTime createdAt;

    private DomainEvent(
            UUID eventId,
            String topic,
            String eventType,
            String aggregateType,
            Long aggregateId,
            String payload,
            EventStatus status
    ) {
        this.id = eventId;
        this.topic = topic;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
        this.retryCount = 0;
        this.createdAt = ZonedDateTime.now();
    }

    public static DomainEvent pending(
            String topic,
            DomainEventEnvelop<?> envelop,
            String payloadJson
    ) {
        return new DomainEvent(
                envelop.getEventId(),
                topic,
                envelop.getEventType(),
                envelop.getAggregateType(),
                envelop.getAggregateId(),
                payloadJson,
                EventStatus.PENDING
        );
    }

    public void markSent() {
        this.status = EventStatus.SENT;
    }

    public void markFailed() {
        this.status = EventStatus.FAILED
        ;
    }

    public void increaseRetryCount() {
        this.retryCount += 1;
    }
}

