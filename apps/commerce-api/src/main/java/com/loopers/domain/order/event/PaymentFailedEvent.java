package com.loopers.domain.order.event;

import com.loopers.domain.order.Payment;

import java.time.LocalDateTime;

public class PaymentFailedEvent {

    private final Long paymentId;
    private final Long orderId;
    private final String userId;
    private final String failureReason;
    private final LocalDateTime occurredAt;

    private PaymentFailedEvent(Payment payment) {
        this.paymentId = payment.getId();
        this.orderId = payment.getOrderId();
        this.userId = payment.getUserId();
        this.failureReason = payment.getFailureReason();
        this.occurredAt = LocalDateTime.now();
    }

    public static PaymentFailedEvent from(Payment payment) {
        return new PaymentFailedEvent(payment);
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
