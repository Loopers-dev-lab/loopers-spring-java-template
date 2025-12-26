package com.loopers.infrastructure.event;

import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class EventHandledRepositoryImpl implements EventHandledRepository {
    private final EventHandledJpaRepository eventHandledJpaRepository;

    @Override
    public boolean existsByEventId(UUID id) {
        return eventHandledJpaRepository.existsById(id);
    }

    @Override
    public void save(EventHandled from) {
        eventHandledJpaRepository.save(from);
    }
}
