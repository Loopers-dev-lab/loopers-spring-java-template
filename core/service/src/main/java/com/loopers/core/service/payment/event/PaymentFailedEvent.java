package com.loopers.core.service.payment.event;

import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.PaymentId;

public record PaymentFailedEvent(PaymentId paymentId, FailedReason failedReason) {
}
