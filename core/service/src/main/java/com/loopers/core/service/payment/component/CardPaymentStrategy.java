package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentType;
import com.loopers.core.service.payment.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.CARD;
    }

    @Override
    public void pay(Payment payment) {
        eventPublisher.publishEvent(new PaymentCreatedEvent(payment.getId(), payment.getType()));
    }
}
