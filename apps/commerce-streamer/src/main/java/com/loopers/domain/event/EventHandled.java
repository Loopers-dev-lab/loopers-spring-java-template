package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "event_handled",
    indexes = {
        @Index(name = "idx_event_id", columnList = "eventId"),
        @Index(name = "idx_handled_at", columnList = "handledAt"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled extends BaseEntity {

  @Column(nullable = false, length = 26)
  private String eventId;

  @Column(nullable = false, length = 200)
  private String businessKey;

  @Column(nullable = false, length = 50)
  private String eventType;

  @Column(nullable = false, length = 100)
  private String topic;

  @Column(nullable = false)
  private LocalDateTime handledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EventStatus status;

  @Column(length = 1000)
  private String errorMessage;

  @Column(nullable = false)
  private Integer retryCount = 0;

  @Column(length = 500)
  private String payload;

  public EventHandled(String eventId, String businessKey, String eventType, String topic, String payload) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.businessKey = businessKey;
    this.topic = topic;
    this.payload = payload;
    this.handledAt = LocalDateTime.now();
    this.status = EventStatus.PENDING;
  }

  public void markAsCompleted() {
    this.status = EventStatus.COMPLETED;
  }

  public void markAsFailed(String errorMessage) {
    this.status = EventStatus.FAILED;
    this.errorMessage = errorMessage;
    this.retryCount++;
  }

  public void markAsProcessing() {
    this.status = EventStatus.PROCESSING;
  }

  public void markAsSkipped() {
    this.status = EventStatus.SKIPPED;
  }

  public boolean canRetry() {
    return retryCount < 3; // 최대 3회 재시도
  }
}
