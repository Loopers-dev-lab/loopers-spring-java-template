package com.loopers.application.common;

import com.loopers.domain.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

  private final DataPlatformClient dataPlatformClient;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(DomainEvent event) {
    dataPlatformClient.send(event);
  }
}
