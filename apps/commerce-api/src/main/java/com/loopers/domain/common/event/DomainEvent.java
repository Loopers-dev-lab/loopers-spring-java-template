package com.loopers.domain.common.event;

import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;

public interface DomainEvent {

  EventType eventType();

  LocalDateTime occurredAt();
}
