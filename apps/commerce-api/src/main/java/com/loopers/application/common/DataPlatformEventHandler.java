package com.loopers.application.common;

import com.loopers.domain.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

  private final DataPlatformClient dataPlatformClient;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(DomainEvent event) {
    try {
      dataPlatformClient.send(event);
    } catch (Exception e) {
      log.error("Failed to send event to data platform. eventType={}", event.eventType(), e);
      throw e;
    }
  }
}
