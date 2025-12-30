package com.loopers.domain.event;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class EventHandledService {

    private final EventHandledRepository eventHandledRepository;

    @Transactional(readOnly = true)
    public boolean isHandled(String eventId) {
        return eventId != null && eventHandledRepository.existsByEventId(eventId);
    }

    @Transactional
    public void markHandled(String eventId, String handler, String topic, String eventType, ZonedDateTime occurredAt) {
        if (eventId == null) {
            return;
        }
        try {
            eventHandledRepository.save(EventHandled.handled(eventId, handler, topic, eventType, occurredAt));
        } catch (DataIntegrityViolationException ignore) {
        }
    }
}
