package com.loopers.domain.eventhandled;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "event_handled",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "domain_type"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled {

    @Id
    private String eventId;

    @Column(name = "domain_type")
    @Enumerated(EnumType.STRING)
    private EventHandledDomainType domainType;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static EventHandled create(String eventId, EventHandledDomainType domainType, String eventType) {
        EventHandled eventHandled = new EventHandled();
        eventHandled.eventId = eventId;
        eventHandled.domainType = domainType;
        eventHandled.eventType = eventType;
        eventHandled.processedAt = LocalDateTime.now();
        return eventHandled;
    }
}
