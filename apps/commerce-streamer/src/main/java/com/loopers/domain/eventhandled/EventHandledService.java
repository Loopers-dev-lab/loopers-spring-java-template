package com.loopers.domain.eventhandled;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventHandledService {

    private final EventHandledRepository eventHandledRepository;

    @Transactional(readOnly = true)
    public boolean isEventHandled(String eventId) {
        return eventHandledRepository.existsByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public boolean isEventHandled(String eventId, EventHandledDomainType domainType) {
        return eventHandledRepository.existsByEventIdAndDomainType(eventId, domainType);
    }

    @Transactional
    public void saveEventHandled(String eventId, EventHandledDomainType domainType, String eventType) {
        EventHandled eventHandled = EventHandled.create(eventId, domainType, eventType);
        eventHandledRepository.save(eventHandled);
    }
}
