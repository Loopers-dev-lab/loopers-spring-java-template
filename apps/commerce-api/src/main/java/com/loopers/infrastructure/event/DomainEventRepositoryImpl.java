package com.loopers.infrastructure.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.DomainEventRepository;
import com.loopers.domain.common.event.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class DomainEventRepositoryImpl implements DomainEventRepository {
    private final DomainEventJpaRepository eventJpaRepository;

    @Override
    public List<DomainEvent> findByStatus(EventStatus status, Pageable pageable) {
        return eventJpaRepository.findByStatus(status, pageable);
    }

    @Override
    public DomainEvent save(DomainEvent event) {
        return eventJpaRepository.save(event);
    }

    @Override
    public Optional<DomainEvent> findById(UUID id) {
        return eventJpaRepository.findById(id);
    }
}
