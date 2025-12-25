package com.loopers.infrastructure.idempotent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_handled")
public class EventHandled {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    private LocalDateTime handledAt;

    protected EventHandled() {
    }

    private EventHandled(String eventId) {
        this.eventId = eventId;
        this.handledAt = LocalDateTime.now();
    }

    public static EventHandled create(String eventId) {
        return new EventHandled(eventId);
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }
}
