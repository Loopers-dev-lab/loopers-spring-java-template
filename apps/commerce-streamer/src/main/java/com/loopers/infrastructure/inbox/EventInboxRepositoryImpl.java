package com.loopers.infrastructure.inbox;

import com.loopers.domain.inbox.EventInbox;
import com.loopers.domain.inbox.EventInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * EventInbox Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class EventInboxRepositoryImpl implements EventInboxRepository {

    private final EventInboxJpaRepository jpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public EventInbox save(EventInbox eventInbox) {
        return jpaRepository.save(eventInbox);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
