package com.loopers.infrastructure.event.outbox;

import com.loopers.domain.event.outbox.OutboxEvent;
import com.loopers.domain.event.outbox.OutboxEventRepository;
import com.loopers.domain.event.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    // fatal marker
    private static Marker FATAL = MarkerFactory.getMarker("FATAL");
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;  // KafkaConfig에서 제공하는 Bean 사용

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 주기적으로 PENDING 상태의 Outbox 이벤트를 Kafka로 발행
     * 5초마다 실행 (필요에 따라 조정)
     * 
     * ⚠️ 토픽별로 Relay를 분리하는 것을 권장합니다:
     * - order-events: 1초 주기 (빠른 전달 필요)
     * - catalog-events: 5초 주기 (느슨해도 괜찮음)
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events to publish", pendingEvents.size());

        int processedCount = 0;
        for (OutboxEvent event : pendingEvents) {
            if (processedCount >= BATCH_SIZE) {
                break;
            }

            String topic = determineTopic(event.getAggregateType());
            String key = event.getAggregateId();

            // Kafka로 메시지 발행
            kafkaTemplate.send(topic, key, event.getPayload()).whenComplete((result, exception) -> {
                if (exception == null) {
                    event.markAsPublished();
                    outboxEventRepository.save(event);
                    log.debug("Published event: eventId={}, topic={}, key={}", event.getId(), topic, key);
                } else {
                    handlePublishFailure(event, exception);
                }
            });

            processedCount++;
        }
    }

    private String determineTopic(String aggregateType) {
        return switch (aggregateType) {
            case "PRODUCT" -> "catalog-events";
            case "ORDER" -> "order-events";
            default -> throw new IllegalArgumentException("Unknown aggregate type: " + aggregateType);
        };
    }

    private void handlePublishFailure(OutboxEvent event, Throwable exception) {
        log.error("Failed to publish event: eventId={}, retryCount={}",
                event.getId(), event.getRetryCount(), exception);

        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            event.markAsFailed(exception.getMessage());
            outboxEventRepository.save(event);
            log.error(FATAL, "Event exceeded max retry count, marked as FAILED: eventId={}", event.getId());
            // TODO: DLQ로 전송하거나 알림 발송
        } else {
            event.incrementRetry();
            outboxEventRepository.save(event);
        }
    }
}

