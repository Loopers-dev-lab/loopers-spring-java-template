package com.loopers.domain.common.event;

import java.time.LocalDateTime;

public interface DomainEvent {

  String eventType();

  LocalDateTime occurredAt();
}
