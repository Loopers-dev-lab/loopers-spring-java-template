package com.loopers.core.domain.event;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.AggregateId;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventOutboxId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.event.vo.PublishedAt;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class EventOutbox {

    private final EventOutboxId id;

    private final EventId eventId;

    private final AggregateType aggregateType;

    private final AggregateId aggregateId;

    private final EventType eventType;

    private final EventOutboxStatus status;

    private final EventPayload payload;

    private final PublishedAt publishedAt;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private EventOutbox(
            EventOutboxId id,
            EventId eventId,
            AggregateType aggregateType,
            AggregateId aggregateId,
            EventType eventType,
            EventOutboxStatus status,
            EventPayload payload,
            PublishedAt publishedAt,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.id = id;
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.status = status;
        this.payload = payload;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EventOutbox create(
            EventId eventId,
            AggregateType aggregateType,
            AggregateId aggregateId,
            EventType eventType,
            EventPayload payload
    ) {
        return new EventOutbox(
                EventOutboxId.empty(),
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                EventOutboxStatus.PENDING,
                payload,
                PublishedAt.empty(),
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static EventOutbox mappedBy(
            EventOutboxId id,
            EventId eventId,
            AggregateType aggregateType,
            AggregateId aggregateId,
            EventType eventType,
            EventOutboxStatus status,
            EventPayload payload,
            PublishedAt publishedAt,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new EventOutbox(id, eventId, aggregateType, aggregateId, eventType, status, payload, publishedAt, createdAt, updatedAt);
    }

    public EventOutbox publish() {
        return this.toBuilder()
                .status(EventOutboxStatus.PUBLISHED)
                .publishedAt(PublishedAt.now())
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
