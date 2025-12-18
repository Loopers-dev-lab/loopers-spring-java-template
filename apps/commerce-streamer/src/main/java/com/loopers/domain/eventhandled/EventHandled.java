package com.loopers.domain.eventhandled;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "event_handled")
public class EventHandled {

  @Id
  @Column(name = "event_id", length = 36, nullable = false)
  private String eventId;

  @Column(name = "handled_at", nullable = false)
  private Instant handledAt;

  protected EventHandled() {}

  private EventHandled(String eventId, Instant handledAt) {
    this.eventId = eventId;
    this.handledAt = handledAt;
  }

  public static EventHandled of(String eventId, Instant handledAt) {
    return new EventHandled(eventId, handledAt);
  }

  public String getEventId() {
    return eventId;
  }

  public Instant getHandledAt() {
    return handledAt;
  }
}
