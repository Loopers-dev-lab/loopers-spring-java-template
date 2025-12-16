package com.loopers.domain.event;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Entity
@Table(name = "event_handled")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
public class EventEntity {
    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "handled_at", nullable = false)
    private ZonedDateTime handledAt;

    private EventEntity(final String eventId, final ZonedDateTime handledAt) {
        this.eventId = eventId;
        this.handledAt = handledAt;
    }

    public static EventEntity create(final String eventId) {
        return new EventEntity(eventId, ZonedDateTime.now());
    }
}
