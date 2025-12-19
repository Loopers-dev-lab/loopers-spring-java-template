package com.loopers.application.event.payment;

import com.loopers.domain.payment.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentCompletedEvent(
    String orderNo,
    Long paymentId,
    PaymentStatus status,
    String failureReason,  // nullable
    LocalDateTime completedAt
) {
    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
}
