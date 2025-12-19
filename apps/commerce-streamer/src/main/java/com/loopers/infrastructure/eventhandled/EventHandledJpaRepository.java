package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledDomainType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, String> {

    boolean existsByEventId(String eventId);

    boolean existsByEventIdAndDomainType(String eventId, EventHandledDomainType domainType);
}
