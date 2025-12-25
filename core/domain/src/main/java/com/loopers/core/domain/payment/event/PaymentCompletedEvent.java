package com.loopers.core.domain.payment.event;

import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.payment.vo.PaymentId;

public record PaymentCompletedEvent(
        EventId eventId,
        PaymentId paymentId
) {
}
