package com.loopers.domain.eventhandled;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class EventHandledService {

    private final EventHandledRepository eventHandledRepository;

    @Transactional
    public boolean isAlreadyHandled(String eventId) {
        return eventHandledRepository.existsByEventId(eventId);
    }

    @Transactional
    public void markAsHandled(String eventId, String eventType, String aggregateKey) {
        EventHandled eventHandled = EventHandled.create(eventId, eventType, aggregateKey);
        eventHandledRepository.saveEventHandled(eventHandled);
    }
}

