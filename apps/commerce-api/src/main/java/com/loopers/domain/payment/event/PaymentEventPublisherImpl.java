package com.loopers.domain.payment.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPaymentProcessingFailed(PaymentProcessingFailedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
