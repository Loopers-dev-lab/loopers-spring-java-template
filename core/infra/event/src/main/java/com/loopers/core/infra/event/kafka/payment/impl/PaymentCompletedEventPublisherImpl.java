package com.loopers.core.infra.event.kafka.payment.impl;

import com.loopers.core.domain.payment.event.PaymentCompletedEvent;
import com.loopers.core.domain.payment.event.PaymentCompletedEventPublisher;
import com.loopers.core.infra.event.kafka.payment.dto.PaymentCompletedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCompletedEventPublisherImpl implements PaymentCompletedEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.payment-completed}")
    private String topic;

    @Override
    public void publish(PaymentCompletedEvent event) {
        kafkaTemplate.send(topic, event.paymentId().value(), PaymentCompletedKafkaEvent.from(event));
    }
}
