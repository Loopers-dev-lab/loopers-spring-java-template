package com.loopers.domain.payment.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EventPublisher 구현체만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    private final EventPublisher eventPublisher;
    
    private static final String TOPIC_PAYMENT_CALLBACK_RECEIVED = "payment.callback-received.v1";
    private static final String TOPIC_PAYMENT_PROCESSED = "payment.completed.v1";
    private static final String TOPIC_PAYMENT_FAILED = "payment.failed.v1";

    @Override
    public void publishPaymentCallbackReceived(PaymentEvents.CallbackReceived event) {
        eventPublisher.publish(TOPIC_PAYMENT_CALLBACK_RECEIVED, String.valueOf(event.orderId()), event);
    }

    @Override
    public void publishPaymentProcessed(PaymentEvents.Processed event) {
        eventPublisher.publish(TOPIC_PAYMENT_PROCESSED, String.valueOf(event.orderId()), event);
    }

    @Override
    public void publishPaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        eventPublisher.publish(TOPIC_PAYMENT_FAILED, String.valueOf(event.orderId()), event);
    }
}
