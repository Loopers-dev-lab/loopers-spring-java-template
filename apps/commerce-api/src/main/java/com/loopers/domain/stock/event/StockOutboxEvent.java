package com.loopers.domain.stock.event;

import com.loopers.domain.event.BaseOutboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_stock", indexes = {
        @Index(name = "idx_outbox_stock_created_at_status", columnList = "createdAt, status"),
        @Index(name = "idx_outbox_stock_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_outbox_stock_status_next_retry", columnList = "status, nextRetryAt")
})
public class StockOutboxEvent extends BaseOutboxEvent {

    @Builder
    public StockOutboxEvent(String eventId, String aggregateId, String type, String payload, String topic) {
        super(eventId, aggregateId, type, payload, topic);
    }
}

