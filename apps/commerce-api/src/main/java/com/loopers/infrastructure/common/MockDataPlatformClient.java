package com.loopers.infrastructure.common;

import com.loopers.application.common.DataPlatformClient;
import com.loopers.domain.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockDataPlatformClient implements DataPlatformClient {

  @Override
  public void send(DomainEvent event) {
    log.info("[DATA_PLATFORM] type={}, occurredAt={}, payload={}",
        event.eventType(), event.occurredAt(), event);
  }
}
