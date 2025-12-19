package com.loopers.domain.payment;

import com.loopers.domain.payment.PaymentEvent.PaymentPaid;

public interface PaymentEventPublisher {
    void publish(PaymentPaid paymentCreated);
    void publish(PaymentEvent.PaymentFailed paymentFailed);
}
