package com.loopers.core.service.payment.event;

import com.loopers.core.domain.payment.vo.PaymentId;

public record PgPaymentCompletedEvent(PaymentId paymentId) {
}
