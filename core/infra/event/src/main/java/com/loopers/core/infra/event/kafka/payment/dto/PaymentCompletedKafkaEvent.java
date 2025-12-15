package com.loopers.core.infra.event.kafka.payment.dto;

import com.loopers.core.domain.payment.event.PaymentCompletedEvent;

public record PaymentCompletedKafkaEvent(
        String eventId,
        String paymentId
) {
    public static PaymentCompletedKafkaEvent from(PaymentCompletedEvent event) {
        return new PaymentCompletedKafkaEvent(event.eventId().value(), event.paymentId().value());
    }
}
