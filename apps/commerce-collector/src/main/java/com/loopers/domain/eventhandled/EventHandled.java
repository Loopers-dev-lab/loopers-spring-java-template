package com.loopers.domain.eventhandled;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "event_handled")
@Getter
@NoArgsConstructor
public class EventHandled extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private ZonedDateTime handledAt;

    @Builder
    public EventHandled(String eventId, String eventType, String aggregateType,
                        String aggregateId, ZonedDateTime handledAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.handledAt = handledAt;
    }

    public static EventHandled create(String eventId, String eventType,
                                      String aggregateType, String aggregateId) {
        return EventHandled.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .handledAt(ZonedDateTime.now())
                .build();
    }
}
