package com.loopers.core.service.productlike;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.product.event.ProductLikeEvent;
import com.loopers.core.domain.productlike.event.ProductLikeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductLikeEventPublishService {

    private final EventOutboxRepository eventOutboxRepository;
    private final ProductLikeEventPublisher publisher;

    public void publish() {
        List<EventOutbox> outboxes = eventOutboxRepository.findAllBy(
                AggregateType.PRODUCT,
                EventType.LIKE_PRODUCT,
                EventOutboxStatus.PENDING
        );

        outboxes.stream()
                .map(EventOutbox::getPayload)
                .map(payload -> JacksonUtil.convertToObject(payload.value(), ProductLikeEvent.class))
                .forEach(publisher::publish);

        outboxes.stream()
                .map(EventOutbox::publish)
                .forEach(eventOutboxRepository::save);
    }
}
