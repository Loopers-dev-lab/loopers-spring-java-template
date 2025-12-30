package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return outboxEventJpaRepository.save(event);
    }

    @Override
    public Optional<OutboxEvent> findByEventId(String eventId) {
        return outboxEventJpaRepository.findByEventId(eventId);
    }

    @Override
    public List<OutboxEvent> findTopPending(int size) {
        int pageSize = Math.max(1, Math.min(size, 100));
        return outboxEventJpaRepository.findByStatusOrderByIdAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, pageSize)
        );
    }
}
