package com.loopers.core.domain.event.repository;

import com.loopers.core.domain.event.EventInbox;
import com.loopers.core.domain.event.vo.EventId;

import java.util.Optional;

public interface EventInboxRepository {

    EventInbox save(EventInbox eventInbox);

    Optional<EventInbox> findByEventId(EventId eventId);
}
