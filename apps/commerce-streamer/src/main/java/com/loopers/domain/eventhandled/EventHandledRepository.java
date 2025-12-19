package com.loopers.domain.eventhandled;

public interface EventHandledRepository {

    boolean existsByEventId(String eventId);

    boolean existsByEventIdAndDomainType(String eventId, EventHandledDomainType domainType);

    EventHandled save(EventHandled eventHandled);
}
