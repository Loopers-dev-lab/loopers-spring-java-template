package com.loopers.application.kafka;

import com.loopers.domain.outbox.EventOutbox;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 발행
 * - Outbox 이벤트를 Kafka로 전송
 * - Topic은 aggregateType에 따라 결정
 * - PartitionKey는 aggregateId로 설정 (순서 보장)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${kafka.topics.catalog-events}")
    private String catalogEventsTopic;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    /**
     * Outbox 이벤트를 Kafka로 발행
     *
     * @param outbox Outbox 이벤트
     * @return CompletableFuture<SendResult>
     */
    public CompletableFuture<SendResult<Object, Object>> publish(EventOutbox outbox) {
        String topic = getTopicByAggregateType(outbox.getAggregateType());
        String partitionKey = outbox.getAggregateId();  // 순서 보장을 위한 Partition Key

        log.info("Kafka 발행 시작 - topic: {}, key: {}, eventType: {}",
            topic, partitionKey, outbox.getEventType());

        return kafkaTemplate.send(topic, partitionKey, outbox.getPayload())
            .thenApply(result -> {
                log.info("Kafka 발행 성공 - topic: {}, partition: {}, offset: {}, eventId: {}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    outbox.getId());
                return result;
            })
            .exceptionally(ex -> {
                log.error("Kafka 발행 실패 - topic: {}, key: {}, eventId: {}, error: {}",
                    topic, partitionKey, outbox.getId(), ex.getMessage(), ex);
                throw new RuntimeException("Kafka 발행 실패", ex);
            });
    }

    /**
     * AggregateType에 따라 Topic 결정
     */
    private String getTopicByAggregateType(String aggregateType) {
        return switch (aggregateType.toUpperCase()) {
            case "ORDER", "PAYMENT" -> orderEventsTopic;
            case "PRODUCT", "LIKE" -> catalogEventsTopic;
            default -> throw new IllegalArgumentException("알 수 없는 AggregateType: " + aggregateType);
        };
    }
}
