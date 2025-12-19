package com.loopers.infrastructure.event.outbox;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxRepository;
import com.loopers.domain.event.outbox.OutboxStatus;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */

@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxRepository {
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus) {
        return outboxEventJpaRepository.findTop100ByStatusOrderByCreatedAtAsc(outboxStatus);
    }

    @Override
    public List<OutboxEventEntity> findTopNByStatusOrderByCreatedAtAsc(OutboxStatus status, int limit) {
        return outboxEventJpaRepository.findTopNByStatusOrderByCreatedAtAsc(status.name(), limit);
    }

    @Override
    public List<OutboxEventEntity> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount) {
        return outboxEventJpaRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(status, maxRetryCount);
    }

    @Override
    public List<OutboxEventEntity> findTopNByStatusAndRetryCountLessThanOrderByCreatedAtAsc(OutboxStatus status,
                                                                                            int maxRetryCount,
                                                                                            int limit) {
        return outboxEventJpaRepository.findTopNByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                status.name(), maxRetryCount, limit);
    }

    @Override
    public OutboxEventEntity save(OutboxEventEntity ready) {
        return outboxEventJpaRepository.save(ready);
    }

    @Override
    public List<OutboxEventEntity> saveAll(List<OutboxEventEntity> events) {
        return outboxEventJpaRepository.saveAll(events);
    }
}
