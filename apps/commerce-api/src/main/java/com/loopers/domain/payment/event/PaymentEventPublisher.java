package com.loopers.domain.payment.event;

public interface PaymentEventPublisher {
    void publishPaymentProcessed(PaymentProcessedEvent event);
    void publishPaymentProcessingFailed(PaymentProcessingFailedEvent event);
}
