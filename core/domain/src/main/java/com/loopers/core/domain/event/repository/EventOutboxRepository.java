package com.loopers.core.domain.event.repository;

import com.loopers.core.domain.event.EventOutbox;

public interface EventOutboxRepository {

    EventOutbox save(EventOutbox eventOutbox);
}
