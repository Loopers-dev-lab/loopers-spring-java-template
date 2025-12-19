package com.loopers.domain.payment.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishPaymentCallbackReceived(PaymentEvents.CallbackReceived event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishPaymentProcessed(PaymentEvents.Processed event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishPaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        eventPublisher.publish(event);
    }
}
