package com.loopers.core.service.product;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.product.event.ProductOutOfStockEvent;
import com.loopers.core.domain.product.event.ProductOutOfStockEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductOutOfStockEventPublishService {

    private final EventOutboxRepository eventOutboxRepository;
    private final ProductOutOfStockEventPublisher publisher;

    @Transactional
    public void publish() {
        List<EventOutbox> outboxes = eventOutboxRepository.findAllBy(
                AggregateType.PRODUCT,
                EventType.OUT_OF_STOCK,
                EventOutboxStatus.PENDING
        );

        outboxes.stream()
                .map(EventOutbox::getPayload)
                .map(payload -> JacksonUtil.convertToObject(payload.value(), ProductOutOfStockEvent.class))
                .forEach(publisher::publish);

        outboxes.stream()
                .map(EventOutbox::publish)
                .forEach(eventOutboxRepository::save);
    }
}
