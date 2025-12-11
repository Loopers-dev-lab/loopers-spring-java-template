package com.loopers.core.domain.payment.vo;

public record PaymentFailedEvent(PaymentId paymentId, FailedReason failedReason) {
}
