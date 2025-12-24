package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxRepository;
import com.loopers.domain.outbox.OutboxType;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KafkaOutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void publishProductViewed() {
        List<OutboxEvent> events = outboxRepository.findPending(10);

        for (OutboxEvent event : events) {
            OutboxType type = event.getEventType();

            if (type.equals(OutboxType.PRODUCT_VIEWED)) {
                kafkaTemplate.send("product-viewed", String.valueOf(event.getAggregateId()), event);
            }

            else if (type.equals(OutboxType.PRODUCT_LIKED)) {
                kafkaTemplate.send("product-liked", String.valueOf(event.getAggregateId()), event);
            }

            else if (type.equals(OutboxType.PRODUCT_SALES)) {
                kafkaTemplate.send("product-sales", String.valueOf(event.getAggregateId()), event);
            }

            event.markAsProcessed();
        }

    }
}
