package com.loopers.domain.payment.event;

import com.loopers.domain.event.BaseInboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_payment", indexes = {
        @Index(name = "idx_inbox_payment_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_inbox_payment_processed_at", columnList = "processedAt")
})
public class PaymentInboxEvent extends BaseInboxEvent {

    @Builder
    public PaymentInboxEvent(String eventId, String aggregateId, String type, String topic) {
        super(eventId, aggregateId, type, topic);
    }
}

