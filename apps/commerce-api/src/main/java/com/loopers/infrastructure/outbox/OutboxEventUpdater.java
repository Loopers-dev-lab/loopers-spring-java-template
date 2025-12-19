package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxEventUpdater {

  private final OutboxEventRepository outboxEventRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int updateStatusToSending(String eventId, Instant leaseExpiry) {
    return outboxEventRepository.updateStatusToSending(eventId, leaseExpiry);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<OutboxEvent> findById(String eventId) {
    return outboxEventRepository.findById(eventId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void toSent(OutboxEvent event) {
    event.toSent();
    outboxEventRepository.save(event);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetToNew(String eventId) {
    outboxEventRepository.resetToNew(eventId);
  }
}