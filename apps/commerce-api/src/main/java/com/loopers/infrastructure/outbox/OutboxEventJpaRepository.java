package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, String> {

  @Query(
      """
      SELECT e FROM OutboxEvent e
      WHERE e.status = 'NEW'
        AND e.nextRetryAt <= :now
      ORDER BY e.occurredAt, e.eventId
      """)
  List<OutboxEvent> findNewEventsReadyToSend(@Param("now") Instant now, Pageable pageable);

  @Modifying
  @Query(
      """
      UPDATE OutboxEvent e
      SET e.status = 'NEW'
      WHERE e.status = 'SENDING'
        AND e.nextRetryAt <= :now
      """)
  int recoverExpiredEvents(@Param("now") Instant now);

  @Modifying
  @Query(
      """
      UPDATE OutboxEvent e
      SET e.status = 'SENDING', e.nextRetryAt = :leaseExpiry
      WHERE e.eventId = :eventId
        AND e.status = 'NEW'
      """)
  int updateStatusToSending(@Param("eventId") String eventId, @Param("leaseExpiry") Instant leaseExpiry);

  @Modifying
  @Query(
      """
      UPDATE OutboxEvent e
      SET e.status = 'NEW'
      WHERE e.eventId = :eventId
        AND e.status = 'SENDING'
      """)
  void resetToNew(@Param("eventId") String eventId);
}
