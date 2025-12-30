package com.loopers.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "event_handled")
@Getter
public class EventHandled {

    @Id
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String handler;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "occurred_at")
    private ZonedDateTime occurredAt;

    @Column(name = "handled_at", nullable = false)
    private ZonedDateTime handledAt;

    protected EventHandled() {
    }

    private EventHandled(
            String eventId,
            String handler,
            String topic,
            String eventType,
            ZonedDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.handler = handler;
        this.topic = topic;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.handledAt = ZonedDateTime.now();
    }

    public static EventHandled handled(
            String eventId,
            String handler,
            String topic,
            String eventType,
            ZonedDateTime occurredAt
    ) {
        return new EventHandled(eventId, handler, topic, eventType, occurredAt);
    }
}
