package com.loopers.domain.common.event;

import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Map;

public interface DomainEvent {

  String eventId();

  EventType eventType();

  LocalDateTime occurredAt();

  String aggregateId();

  default Map<String, Object> payload() {
    return Map.of();
  }
}
