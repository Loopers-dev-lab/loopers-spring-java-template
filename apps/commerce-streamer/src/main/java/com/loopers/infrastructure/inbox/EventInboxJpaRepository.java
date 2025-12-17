package com.loopers.infrastructure.inbox;

import com.loopers.domain.inbox.EventInbox;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * EventInbox JPA Repository
 */
public interface EventInboxJpaRepository extends JpaRepository<EventInbox, Long> {

    boolean existsByEventId(String eventId);
}
