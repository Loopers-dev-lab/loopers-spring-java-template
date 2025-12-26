package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.application.metrics.MetricsAggregationService;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.infrastructure.kafka.DlqPublisher;
import com.loopers.infrastructure.kafka.event.KafkaEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Product Metrics Kafka Consumer
 * - Product 관련 이벤트를 수신하여 메트릭 집계
 * - Manual Ack: 처리 성공 후에만 offset commit
 * - Batch Listener: 한 번에 여러 메시지 처리
 * - 에러 처리: 복구 불가능한 에러는 DLQ로 전송, 일시적 에러는 Kafka 재시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsConsumer {

    private final MetricsAggregationService aggregationService;
    private final ObjectMapper objectMapper;
    private final DlqPublisher dlqPublisher;

    /**
     * 상품 좋아요 이벤트 Consumer
     */
    @KafkaListener(
        topics = {"loopers.commerce.product-liked-v1"},
        groupId = "metrics-aggregator",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeProductLiked(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    ) {
        log.info("[Consumer] Received {} product-liked events", records.size());

        List<ConsumerRecord<String, String>> failedRecords = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                KafkaEventEnvelope<ProductLikedEvent> envelope =
                    objectMapper.readValue(
                        record.value(),
                        new TypeReference<KafkaEventEnvelope<ProductLikedEvent>>() {}
                    );

                aggregationService.handleProductLiked(
                    envelope.eventId(),
                    envelope.payload()
                );

            } catch (Exception e) {
                log.error("[Consumer] Failed to process product-liked event - offset: {}, key: {}",
                    record.offset(), record.key(), e);
                handleFailedRecord(record, e, failedRecords);
            }
        }

        // 실패한 레코드가 있으면 예외를 던져서 재처리
        if (!failedRecords.isEmpty()) {
            log.warn("[Consumer] {} records failed, will retry", failedRecords.size());
            throw new RuntimeException(
                String.format("Failed to process %d records", failedRecords.size())
            );
        }

        ack.acknowledge(); // 모두 성공 시에만 커밋
        log.debug("[Consumer] Acknowledged {} product-liked events", records.size());
    }

    /**
     * 상품 좋아요 취소 이벤트 Consumer
     */
    @KafkaListener(
        topics = {"loopers.commerce.product-unliked-v1"},
        groupId = "metrics-aggregator",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeProductUnliked(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    ) {
        log.info("[Consumer] Received {} product-unliked events", records.size());

        List<ConsumerRecord<String, String>> failedRecords = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                KafkaEventEnvelope<ProductUnlikedEvent> envelope =
                    objectMapper.readValue(
                        record.value(),
                        new TypeReference<KafkaEventEnvelope<ProductUnlikedEvent>>() {}
                    );

                aggregationService.handleProductUnliked(
                    envelope.eventId(),
                    envelope.payload()
                );

            } catch (Exception e) {
                log.error("[Consumer] Failed to process product-unliked event - offset: {}, key: {}",
                    record.offset(), record.key(), e);
                handleFailedRecord(record, e, failedRecords);
            }
        }

        if (!failedRecords.isEmpty()) {
            log.warn("[Consumer] {} records failed, will retry", failedRecords.size());
            throw new RuntimeException(
                String.format("Failed to process %d records", failedRecords.size())
            );
        }

        ack.acknowledge();
        log.debug("[Consumer] Acknowledged {} product-unliked events", records.size());
    }

    /**
     * 주문 완료 이벤트 Consumer (판매량 집계용)
     */
    @KafkaListener(
        topics = {"loopers.commerce.order-completed-v1"},
        groupId = "metrics-aggregator",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeOrderCompleted(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    ) {
        log.info("[Consumer] Received {} order-completed events", records.size());

        List<ConsumerRecord<String, String>> failedRecords = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                KafkaEventEnvelope<OrderCompletedEvent> envelope =
                    objectMapper.readValue(
                        record.value(),
                        new TypeReference<KafkaEventEnvelope<OrderCompletedEvent>>() {}
                    );

                aggregationService.handleOrderCompleted(
                    envelope.eventId(),
                    envelope.payload()
                );

            } catch (Exception e) {
                log.error("[Consumer] Failed to process order-completed event - offset: {}, key: {}",
                    record.offset(), record.key(), e);
                handleFailedRecord(record, e, failedRecords);
            }
        }

        if (!failedRecords.isEmpty()) {
            log.warn("[Consumer] {} records failed, will retry", failedRecords.size());
            throw new RuntimeException(
                String.format("Failed to process %d records", failedRecords.size())
            );
        }

        ack.acknowledge();
        log.debug("[Consumer] Acknowledged {} order-completed events", records.size());
    }

    /**
     * 상품 조회 이벤트 Consumer (조회수 집계용)
     */
    @KafkaListener(
        topics = {"loopers.commerce.product-viewed-v1"},
        groupId = "metrics-aggregator",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeProductViewed(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    ) {
        log.info("[Consumer] Received {} product-viewed events", records.size());

        List<ConsumerRecord<String, String>> failedRecords = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                KafkaEventEnvelope<ProductViewedEvent> envelope =
                    objectMapper.readValue(
                        record.value(),
                        new TypeReference<KafkaEventEnvelope<ProductViewedEvent>>() {}
                    );

                aggregationService.handleProductViewed(
                    envelope.eventId(),
                    envelope.payload()
                );

            } catch (Exception e) {
                log.error("[Consumer] Failed to process product-viewed event - offset: {}, key: {}",
                    record.offset(), record.key(), e);
                handleFailedRecord(record, e, failedRecords);
            }
        }

        if (!failedRecords.isEmpty()) {
            log.warn("[Consumer] {} records failed, will retry", failedRecords.size());
            throw new RuntimeException(
                String.format("Failed to process %d records", failedRecords.size())
            );
        }

        ack.acknowledge();
        log.debug("[Consumer] Acknowledged {} product-viewed events", records.size());
    }

    /**
     * 실패한 레코드 처리 공통 메서드
     * - 복구 불가능한 에러: DLQ로 전송
     * - 일시적 에러: 배치 재처리 (Kafka가 자동으로 재시도)
     */
    private void handleFailedRecord(
        ConsumerRecord<String, String> record,
        Exception exception,
        List<ConsumerRecord<String, String>> failedRecords
    ) {
        if (dlqPublisher.shouldSendToDlq(exception)) {
            // 복구 불가능한 에러 → 즉시 DLQ로 전송
            dlqPublisher.publishToDlq(record, exception);
        } else {
            // 일시적 에러 → 재시도 대상에 추가
            failedRecords.add(record);
        }
    }
}
