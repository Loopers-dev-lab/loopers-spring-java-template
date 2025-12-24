package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OutboxEvent extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private AggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    public OutboxEvent(AggregateType aggregateType, Long aggregateId, OutboxType eventType, OutboxStatus status) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.status = status;
    }

    public static OutboxEvent of(AggregateType aggregateType, Long aggregateId, OutboxType eventType) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, OutboxStatus.PENDING);
    }

    public void markAsProcessed() {
        this.status = OutboxStatus.PROCESSED;
    }

}
