package com.loopers.domain.order.event;

import com.loopers.domain.order.Payment;

import java.time.LocalDateTime;

public class PaymentCompletedEvent {

    private final Long paymentId;
    private final Long orderId;
    private final String userId;
    private final String pgTransactionId;
    private final LocalDateTime occurredAt;

    private PaymentCompletedEvent(Payment payment) {
        this.paymentId = payment.getId();
        this.orderId = payment.getOrderId();
        this.userId = payment.getUserId();
        this.pgTransactionId = payment.getPgTransactionId();
        this.occurredAt = LocalDateTime.now();
    }

    public static PaymentCompletedEvent from(Payment payment) {
        return new PaymentCompletedEvent(payment);
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

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
