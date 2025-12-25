package com.loopers.core.infra.database.mysql.event.impl;

import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.infra.database.mysql.event.EventOutboxJpaRepository;
import com.loopers.core.infra.database.mysql.event.entity.EventOutboxEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventOutboxRepositoryImpl implements EventOutboxRepository {

    private final EventOutboxJpaRepository repository;

    @Override
    public EventOutbox save(EventOutbox eventOutbox) {
        return repository.save(EventOutboxEntity.from(eventOutbox)).to();
    }

    @Override
    public List<EventOutbox> findAllBy(AggregateType aggregateType, EventType eventType, EventOutboxStatus status) {
        return repository.findAllByEventTypeAndAggregateTypeAndStatus(eventType.name(), aggregateType.name(), status.name()).stream()
                .map(EventOutboxEntity::to)
                .toList();
    }
}
