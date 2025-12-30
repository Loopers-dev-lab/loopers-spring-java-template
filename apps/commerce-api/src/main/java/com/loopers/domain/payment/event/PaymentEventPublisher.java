package com.loopers.domain.payment.event;

public interface PaymentEventPublisher {
    void publishPaymentCallbackReceived(PaymentEvents.CallbackReceived event);
    void publishPaymentProcessed(PaymentEvents.Processed event);
    void publishPaymentProcessingFailed(PaymentEvents.ProcessingFailed event);
}
