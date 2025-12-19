package com.loopers.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "outbox_event",
    indexes = {
      @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_at"),
      @Index(name = "idx_outbox_aggregate", columnList = "event_type, aggregate_id, occurred_at")
    })
public class OutboxEvent {

  @Id
  @Column(name = "event_id", length = 36, nullable = false)
  private String eventId;

  @Column(name = "topic", length = 100, nullable = false)
  private String topic;

  @Column(name = "event_type", length = 50, nullable = false)
  private String eventType;

  @Column(name = "aggregate_id", length = 50, nullable = false)
  private String aggregateId;

  @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private Long occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 10, nullable = false)
  private OutboxStatus status;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount;

  @Column(name = "next_retry_at", nullable = false)
  private Instant nextRetryAt;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  protected OutboxEvent() {}

  private OutboxEvent(
      String eventId,
      String topic,
      String eventType,
      String aggregateId,
      String payload,
      Long occurredAt,
      OutboxStatus status,
      Integer retryCount,
      Instant nextRetryAt,
      String lastError) {
    this.eventId = eventId;
    this.topic = topic;
    this.eventType = eventType;
    this.aggregateId = aggregateId;
    this.payload = payload;
    this.occurredAt = occurredAt;
    this.status = status;
    this.retryCount = retryCount;
    this.nextRetryAt = nextRetryAt;
    this.lastError = lastError;
  }

  public static OutboxEvent create(
      String eventId,
      String topic,
      String eventType,
      String aggregateId,
      String payload,
      Long occurredAt) {
    return new OutboxEvent(
        eventId,
        topic,
        eventType,
        aggregateId,
        payload,
        occurredAt,
        OutboxStatus.NEW,
        0,
        Instant.now(),
        null);
  }

  public String getEventId() {
    return eventId;
  }

  public String getTopic() {
    return topic;
  }

  public String getEventType() {
    return eventType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getPayload() {
    return payload;
  }

  public Long getOccurredAt() {
    return occurredAt;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void toSent() {
    this.status = OutboxStatus.SENT;
  }

  public void toDead() {
    this.status = OutboxStatus.DEAD;
  }

  public boolean scheduleRetry(int maxRetry, String error, Instant nextRetry) {
    if (exceedsMaxRetry(maxRetry)) {
      return false;
    }
    this.retryCount++;
    this.lastError = error;
    this.nextRetryAt = nextRetry;
    this.status = OutboxStatus.NEW;
    return true;
  }

  public boolean exceedsMaxRetry(int maxRetry) {
    return this.retryCount >= maxRetry;
  }
}
