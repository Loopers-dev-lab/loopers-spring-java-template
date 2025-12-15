package com.loopers.core.service.payment;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.payment.event.PaymentCompletedEvent;
import com.loopers.core.domain.payment.event.PaymentCompletedEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCompletedEventPublishService {

    private final EventOutboxRepository eventOutboxRepository;
    private final PaymentCompletedEventPublisher publisher;

    @Transactional
    public void publish() {
        List<EventOutbox> outboxes = eventOutboxRepository.findAllBy(
                AggregateType.PAYMENT,
                EventType.PAYMENT_COMPLETED,
                EventOutboxStatus.PENDING
        );

        outboxes.stream()
                .map(EventOutbox::getPayload)
                .map(payload -> JacksonUtil.convertToObject(payload.value(), PaymentCompletedEvent.class))
                .forEach(publisher::publish);

        outboxes.stream()
                .map(EventOutbox::publish)
                .forEach(eventOutboxRepository::save);
    }
}
