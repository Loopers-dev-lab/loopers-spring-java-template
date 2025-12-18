package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {


  @Column(nullable = false, length = 100)
  private String aggregateType;

  @Column(nullable = false, length = 100)
  private String aggregateId;

  @Column(nullable = false, length = 100)
  private String eventType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OutboxEventStatus status;

  @Column
  private LocalDateTime processedAt;

  @Column(length = 500)
  private String errorMessage;

  @Column(nullable = false)
  private Integer retryCount = 0;

  public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.status = OutboxEventStatus.PENDING;
  }

  public void markAsProcessed() {
    this.status = OutboxEventStatus.PROCESSED;
    this.processedAt = LocalDateTime.now();
  }

  public void markAsFailed(String errorMessage) {
    this.status = OutboxEventStatus.FAILED;
    this.errorMessage = errorMessage;
    this.retryCount++;
  }

  public void retry() {
    this.status = OutboxEventStatus.PENDING;
    this.errorMessage = null;
  }

  public boolean canRetry() {
    return retryCount < 3; // 최대 3회 재시도
  }
}
