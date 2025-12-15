package com.loopers.core.domain.payment.event;

public interface PaymentCompletedEventPublisher {

    void publish(PaymentCompletedEvent event);
}
