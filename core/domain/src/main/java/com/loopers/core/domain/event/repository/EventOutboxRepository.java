package com.loopers.core.domain.event.repository;

import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;

import java.util.List;

public interface EventOutboxRepository {

    EventOutbox save(EventOutbox eventOutbox);

    List<EventOutbox> findAllBy(AggregateType aggregateType, EventType eventType, EventOutboxStatus status);
}
