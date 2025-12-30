package com.loopers.domain.stock.event;

import com.loopers.domain.event.BaseInboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_stock", indexes = {
        @Index(name = "idx_inbox_stock_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_inbox_stock_processed_at", columnList = "processedAt")
})
public class StockInboxEvent extends BaseInboxEvent {

    @Builder
    public StockInboxEvent(String eventId, String aggregateId, String type, String topic) {
        super(eventId, aggregateId, type, topic);
    }
}

