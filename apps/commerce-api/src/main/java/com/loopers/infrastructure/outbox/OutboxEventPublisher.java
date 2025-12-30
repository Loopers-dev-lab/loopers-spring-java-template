package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;
    @Value("${outbox.publisher.enabled:true}")
    private boolean publisherEnabled;

    @PostConstruct
    void logConfiguration() {
        log.info("OutboxEventPublisher enabled={}, batchSize={}", publisherEnabled, batchSize);
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        if (!publisherEnabled) {
            return;
        }

        List<OutboxEvent> events = outboxService.fetchPending(batchSize);
        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            try {
                JsonNode payload = objectMapper.readTree(event.getPayload());
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload).get();
                outboxService.markSent(event);
            } catch (Exception exception) {
                log.error("Outbox publish failed eventId={} topic={}", event.getEventId(), event.getTopic(), exception);
                outboxService.markFailed(event, truncateMessage(exception.getMessage()));
            }
        }
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
