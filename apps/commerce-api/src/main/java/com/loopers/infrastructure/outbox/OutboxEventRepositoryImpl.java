package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

  private final OutboxEventJpaRepository jpaRepository;

  @Override
  public void save(OutboxEvent event) {
    jpaRepository.save(event);
  }

  @Override
  public List<OutboxEvent> findNewEventsReadyToSend(Instant now, int limit) {
    return jpaRepository.findNewEventsReadyToSend(now, PageRequest.of(0, limit));
  }

  @Override
  public int recoverExpiredEvents(Instant now) {
    return jpaRepository.recoverExpiredEvents(now);
  }

  @Override
  public int updateStatusToSending(String eventId, Instant leaseExpiry) {
    return jpaRepository.updateStatusToSending(eventId, leaseExpiry);
  }

  @Override
  public Optional<OutboxEvent> findById(String eventId) {
    return jpaRepository.findById(eventId);
  }

  @Override
  public void resetToNew(String eventId) {
    jpaRepository.resetToNew(eventId);
  }
}
