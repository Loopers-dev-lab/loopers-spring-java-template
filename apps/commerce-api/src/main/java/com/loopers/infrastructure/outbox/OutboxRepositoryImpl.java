package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxEventRepository {
  private final OutboxJpaRepository outboxJpaRepository;

  @Override
  public Optional<OutboxEvent> findById(Long eventId) {
    return outboxJpaRepository.findById(eventId);
  }

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    return outboxJpaRepository.save(outboxEvent);
  }
  @Override
  public void deleteAll( List<OutboxEvent> outboxEventList) {
    outboxJpaRepository.deleteAll(outboxEventList);
  }


  @Override
  public List<OutboxEvent> findByStatusOrderByAggregateAndCreatedAt(OutboxEventStatus status) {
    return outboxJpaRepository.findByStatusOrderByAggregateAndCreatedAt(status);
  }

  @Override
  public List<OutboxEvent> findPendingEventsOrderedByAggregate() {
    return outboxJpaRepository.findPendingEventsOrderedByAggregate();
  }

  @Override
  public List<OutboxEvent> findFailedEventsForRetry(ZonedDateTime since) {
    return outboxJpaRepository.findFailedEventsForRetry(since);
  }

  @Override
  public List<OutboxEvent> findProcessedEventsBefore(LocalDateTime before) {
    return outboxJpaRepository.findProcessedEventsBefore(before);
  }

  @Override
  public List<OutboxEvent> findByAggregateTypeAndAggregateIdAndStatusOrderByCreatedAt(String aggregateType, String aggregateId, OutboxEventStatus status) {
    return outboxJpaRepository.findByAggregateTypeAndAggregateIdAndStatusOrderByCreatedAt(aggregateType, aggregateId, status);
  }
}
