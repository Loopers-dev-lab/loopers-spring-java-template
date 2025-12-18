package com.loopers.infrastructure.event;

import org.springframework.stereotype.Component;

import com.loopers.domain.event.EventEntity;
import com.loopers.domain.event.EventRepository;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Component
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {
    private final EventJpaRepository eventJpaRepository;

    @Override
    public EventEntity save(EventEntity eventEntity) {
        return eventJpaRepository.save(eventEntity);
    }
    
    @Override
    public void deleteAll() {
        eventJpaRepository.deleteAll();
    }
    
    @Override
    public boolean existsById(String eventId) {
        return eventJpaRepository.existsById(eventId);
    }
}
