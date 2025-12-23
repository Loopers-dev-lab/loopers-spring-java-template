package com.loopers.domain.eventhandled;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "event_handled")
@Getter
public class EventHandled extends BaseEntity {

    @Column(nullable = false, unique = true, length = 200)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String aggregateKey;

    @Builder
    private EventHandled(String eventId, String eventType, String aggregateKey) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateKey = aggregateKey;
    }

    public EventHandled() {
    }

    public static EventHandled create(String eventId, String eventType, String aggregateKey) {
        return EventHandled.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateKey(aggregateKey)
                .build();
    }
}

