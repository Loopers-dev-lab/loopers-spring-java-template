package com.loopers.core.service.payment.event;

import com.loopers.core.domain.payment.Payment;

public record PgPaymentEvent(Payment payment) {
}
