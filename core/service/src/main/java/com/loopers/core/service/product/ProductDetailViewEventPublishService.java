package com.loopers.core.service.product;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.product.event.ProductDetailViewEvent;
import com.loopers.core.domain.product.event.ProductDetailViewEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductDetailViewEventPublishService {

    private final EventOutboxRepository eventOutboxRepository;
    private final ProductDetailViewEventPublisher publisher;

    @Transactional
    public void publish() {
        List<EventOutbox> outboxes = eventOutboxRepository.findAllBy(
                AggregateType.PRODUCT,
                EventType.VIEW_PRODUCT_DETAIL,
                EventOutboxStatus.PENDING
        );

        outboxes.stream()
                .map(EventOutbox::getPayload)
                .map(payload -> JacksonUtil.convertToObject(payload.value(), ProductDetailViewEvent.class))
                .forEach(publisher::publish);

        outboxes.stream()
                .map(EventOutbox::publish)
                .forEach(eventOutboxRepository::save);
    }
}
