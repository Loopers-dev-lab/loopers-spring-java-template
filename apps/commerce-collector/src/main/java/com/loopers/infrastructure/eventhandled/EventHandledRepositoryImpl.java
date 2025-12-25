package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventHandledRepositoryImpl implements EventHandledRepository {
    private final EventHandledJpaRepository eventHandledJpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return eventHandledJpaRepository.existsByEventId(eventId);
    }

    @Override
    public EventHandled save(EventHandled eventHandled) {
        return eventHandledJpaRepository.save(eventHandled);
    }

    @Override
    public List<EventHandled> findByEventId(String eventId) {
        return eventHandledJpaRepository.findByEventId(eventId);
    }
}
