package com.loopers.domain.event.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String aggregateType;  // ORDER, PRODUCT 등

    @Column(nullable = false, length = 100)
    private String aggregateId;    // 주문 ID, 상품 ID 등

    @Column(nullable = false, length = 100)
    private String eventType;      // OrderCreated, ProductLiked 등

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;          // JSON 형태의 이벤트 데이터

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Builder
    public OutboxEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.lastErrorMessage = errorMessage;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}

