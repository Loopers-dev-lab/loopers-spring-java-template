package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxEventUpdater {

  private final OutboxEventRepository outboxEventRepository;

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