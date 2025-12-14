package com.loopers.domain.common.event;

public interface DomainEventPublisher {

  void publish(DomainEvent event);
}