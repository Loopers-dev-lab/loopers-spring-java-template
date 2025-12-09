package com.loopers.core.infra.database.mysql.event.impl;

import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.infra.database.mysql.event.EventOutboxJpaRepository;
import com.loopers.core.infra.database.mysql.event.entity.EventOutboxEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventOutboxRepositoryImpl implements EventOutboxRepository {

    private final EventOutboxJpaRepository repository;

    @Override
    public EventOutbox save(EventOutbox eventOutbox) {
        return repository.save(EventOutboxEntity.from(eventOutbox)).to();
    }
}
