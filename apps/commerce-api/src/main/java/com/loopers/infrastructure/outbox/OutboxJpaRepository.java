package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxEvent, Long> {
  @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.aggregateType, o.aggregateId, o.createdAt ASC")
  List<OutboxEvent> findByStatusOrderByAggregateAndCreatedAt(OutboxEventStatus status);

  @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' ORDER BY o.aggregateType, o.aggregateId, o.createdAt ASC")
  List<OutboxEvent> findPendingEventsOrderedByAggregate();

  @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' AND o.retryCount < 3 AND o.createdAt > :since ORDER BY o.aggregateType, o.aggregateId, o.createdAt ASC")
  List<OutboxEvent> findFailedEventsForRetry(ZonedDateTime since);

  @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PROCESSED' AND o.processedAt < :before")
  List<OutboxEvent> findProcessedEventsBefore(LocalDateTime before);

  @Query("SELECT o FROM OutboxEvent o WHERE o.aggregateType = :aggregateType AND o.aggregateId = :aggregateId AND o.status = :status ORDER BY o.createdAt ASC")
  List<OutboxEvent> findByAggregateTypeAndAggregateIdAndStatusOrderByCreatedAt(String aggregateType, String aggregateId, OutboxEventStatus status);

}
