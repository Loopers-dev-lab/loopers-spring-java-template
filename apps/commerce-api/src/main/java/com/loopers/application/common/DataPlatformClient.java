package com.loopers.application.common;

import com.loopers.domain.common.event.DomainEvent;

public interface DataPlatformClient {

  void send(DomainEvent event);
}
