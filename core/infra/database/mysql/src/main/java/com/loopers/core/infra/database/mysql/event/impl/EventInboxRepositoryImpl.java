package com.loopers.core.infra.database.mysql.event.impl;

import com.loopers.core.domain.event.EventInbox;
import com.loopers.core.domain.event.repository.EventInboxRepository;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.infra.database.mysql.event.EventInboxJpaRepository;
import com.loopers.core.infra.database.mysql.event.entity.EventInboxEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventInboxRepositoryImpl implements EventInboxRepository {

    private final EventInboxJpaRepository eventInboxJpaRepository;

    @Override
    public EventInbox save(EventInbox eventInbox) {
        EventInboxEntity entity = EventInboxEntity.from(eventInbox);
        EventInboxEntity saved = eventInboxJpaRepository.save(entity);
        return saved.to();
    }

    @Override
    public Optional<EventInbox> findByEventId(EventId eventId) {
        return eventInboxJpaRepository
                .findByEventId(eventId.value())
                .map(EventInboxEntity::to);
    }
}
