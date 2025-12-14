package com.loopers.domain.common;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.DomainEventPublisher;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class AggregateRoot extends BaseEntity {

  @Transient
  private List<DomainEvent> domainEvents = new ArrayList<>();

  protected void registerEvent(DomainEvent event) {
    if (domainEvents == null) {
      domainEvents = new ArrayList<>();
    }
    this.domainEvents.add(event);
  }

  public void publishEvents(DomainEventPublisher publisher) {
    getDomainEvents().forEach(publisher::publish);
    clearDomainEvents();
  }

  public List<DomainEvent> getDomainEvents() {
    if (domainEvents == null) {
      domainEvents = new ArrayList<>();
    }
    return Collections.unmodifiableList(domainEvents);
  }

  public void clearDomainEvents() {
    if (domainEvents != null) {
      domainEvents.clear();
    }
  }
}