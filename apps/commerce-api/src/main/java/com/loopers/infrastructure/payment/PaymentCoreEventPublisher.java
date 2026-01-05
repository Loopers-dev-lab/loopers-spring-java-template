package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentEvent.PaymentPaid;
import com.loopers.domain.payment.PaymentEvent.PaymentFailed;
import com.loopers.domain.payment.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentCoreEventPublisher implements PaymentEventPublisher {

    private static final String paidTopic = "payment.paid";
    private static final String failedTopic = "payment.failed";
    private final KafkaTemplate<Object, Object> kafkaTemplate;


    @Override
    public void publish(final PaymentPaid paymentCreated) {
        kafkaTemplate.send(paidTopic, paymentCreated.payment().getId(), paymentCreated);
    }

    @Override
    public void publish(final PaymentFailed paymentFailed) {
        kafkaTemplate.send(failedTopic, paymentFailed.payment().getId(), paymentFailed);
    }
}
