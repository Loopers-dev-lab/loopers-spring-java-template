package com.loopers.domain.outbox;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
  Optional<OutboxEvent> findById(Long eventId);

  OutboxEvent save(OutboxEvent outboxEvent);

  List<OutboxEvent> findByStatusOrderByAggregateAndCreatedAt(OutboxEventStatus status);

  List<OutboxEvent> findPendingEventsOrderedByAggregate();

  List<OutboxEvent> findFailedEventsForRetry(ZonedDateTime since);

  List<OutboxEvent> findProcessedEventsBefore(LocalDateTime before);

  List<OutboxEvent> findByAggregateTypeAndAggregateIdAndStatusOrderByCreatedAt(String aggregateType, String aggregateId, OutboxEventStatus status);

  void deleteAll(List<OutboxEvent> oldProcessedEvents);
}
