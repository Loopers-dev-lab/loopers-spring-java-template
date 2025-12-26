package com.loopers.domain.event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_handled")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled {

    @Id
    private UUID id;

    private Instant handledAt;

    public EventHandled(UUID eventId) {
        this.id = eventId;
        this.handledAt = Instant.now();
    }

    public static EventHandled from (UUID eventId) {
        return new EventHandled(eventId);
    }
}
