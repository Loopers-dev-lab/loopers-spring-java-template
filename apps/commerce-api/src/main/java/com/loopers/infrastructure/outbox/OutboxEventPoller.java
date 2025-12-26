package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.infrastructure.kafka.KafkaTopicRouter;
import com.loopers.infrastructure.kafka.event.KafkaEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Outbox Event Poller
 * - 주기적으로 PENDING 상태의 이벤트를 조회하여 Kafka로 발행
 * - 5초마다 실행 (fixedDelay)
 * - At Least Once 보장: acks=all, idempotence=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPoller {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicRouter topicRouter;
    private final ObjectMapper objectMapper;

    /**
     * PENDING 이벤트 폴링 및 Kafka 발행
     * - 5초마다 실행
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
            OutboxEvent.OutboxStatus.PENDING,
            BATCH_SIZE
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[OutboxPoller] Processing {} pending events", pendingEvents.size());

        int successCount = 0;
        int failureCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                publishToKafka(event);
                event.markAsPublished();
                outboxRepository.save(event);
                successCount++;

            } catch (Exception e) {
                log.error("[OutboxPoller] Failed to publish event: id={}, type={}",
                    event.getId(), event.getEventType(), e);
                event.markAsFailed(e.getMessage());
                outboxRepository.save(event);
                failureCount++;
            }
        }

        log.info("[OutboxPoller] Completed - success: {}, failed: {}", successCount, failureCount);
    }

    /**
     * FAILED 이벤트 재시도
     * - 30초마다 실행
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> retryableEvents = outboxRepository.findRetryableEvents(
            MAX_RETRY_COUNT,
            BATCH_SIZE
        );

        if (retryableEvents.isEmpty()) {
            return;
        }

        log.info("[OutboxPoller] Retrying {} failed events", retryableEvents.size());

        int successCount = 0;
        int failureCount = 0;

        for (OutboxEvent event : retryableEvents) {
            try {
                publishToKafka(event);
                event.markAsPublished();
                outboxRepository.save(event);
                successCount++;
                log.info("[OutboxPoller] Retry success - id: {}, retryCount: {}",
                    event.getId(), event.getRetryCount());

            } catch (Exception e) {
                event.markAsFailed(e.getMessage());
                outboxRepository.save(event);
                failureCount++;
                log.warn("[OutboxPoller] Retry failed - id: {}, retryCount: {}/{}",
                    event.getId(), event.getRetryCount(), MAX_RETRY_COUNT);
            }
        }

        log.info("[OutboxPoller] Retry completed - success: {}, failed: {}", successCount, failureCount);
    }

    /**
     * Outbox 이벤트를 Kafka로 발행
     */
    private void publishToKafka(OutboxEvent outboxEvent) throws Exception {
        String topic = topicRouter.getTopicName(outboxEvent.getEventType());
        String key = outboxEvent.getPartitionKey();

        // Envelope로 감싸서 전송
        KafkaEventEnvelope<Object> envelope = new KafkaEventEnvelope<>(
            String.valueOf(outboxEvent.getId()),
            outboxEvent.getEventType(),
            outboxEvent.getPartitionKey(),
            objectMapper.readValue(outboxEvent.getPayload(), Object.class),
            outboxEvent.getCreatedAt()
        );

        // 동기 전송 (실패 시 예외 발생)
        try {
            kafkaTemplate.send(topic, key, envelope).get();

            log.debug("[OutboxPoller] Published to Kafka - topic: {}, key: {}, eventId: {}",
                topic, key, outboxEvent.getId());

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Kafka send failed", e);
        }
    }
}
