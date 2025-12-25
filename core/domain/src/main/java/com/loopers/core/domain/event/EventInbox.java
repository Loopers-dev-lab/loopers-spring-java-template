package com.loopers.core.domain.event;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventInboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.AggregateId;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventInboxId;
import com.loopers.core.domain.event.vo.EventPayload;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class EventInbox {

    private final EventInboxId id;

    private final EventId eventId;

    private final AggregateType aggregateType;

    private final AggregateId aggregateId;

    private final EventType eventType;

    private final EventInboxStatus status;

    private final EventPayload payload;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private EventInbox(
            EventInboxId id,
            EventId eventId,
            AggregateType aggregateType,
            AggregateId aggregateId,
            EventType eventType,
            EventInboxStatus status,
            EventPayload payload,
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EventInbox create(
            EventId eventId,
            AggregateType aggregateType,
            AggregateId aggregateId,
            EventType eventType,
            EventPayload payload
    ) {
        return new EventInbox(
                EventInboxId.empty(),
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                EventInboxStatus.PENDING,
                payload,
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static EventInbox mappedBy(
            EventInboxId id,
            EventId eventId,
            AggregateType aggregateType,
            AggregateId aggregateId,
            EventType eventType,
            EventInboxStatus status,
            EventPayload payload,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new EventInbox(id, eventId, aggregateType, aggregateId, eventType, status, payload, createdAt, updatedAt);
    }

    public EventInbox process() {
        return this.toBuilder()
                .status(EventInboxStatus.PROCESSED)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public EventInbox fail() {
        return this.toBuilder()
                .status(EventInboxStatus.FAILED)
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
