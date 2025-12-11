package com.loopers.domain.payment.event;

public interface PaymentEventPublisher {
    void publishPaymentProcessed(PaymentEvents.Processed event);
    void publishPaymentProcessingFailed(PaymentEvents.ProcessingFailed event);
}
