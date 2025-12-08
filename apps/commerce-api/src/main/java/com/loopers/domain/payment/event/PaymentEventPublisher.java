package com.loopers.domain.payment.event;

public interface PaymentEventPublisher {
    void publishPaymentProcess(PaymentProcessEvent event);
}
