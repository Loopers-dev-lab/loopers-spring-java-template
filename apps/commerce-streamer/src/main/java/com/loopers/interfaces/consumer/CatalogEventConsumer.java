package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.dlq.DeadLetterQueueService;
import com.loopers.application.inbox.EventInboxService;
import com.loopers.application.metrics.ProductMetricsService;
import com.loopers.application.ranking.RankingAggregator;
import com.loopers.confg.kafka.KafkaConfig;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalog 이벤트 Consumer
 * - catalog-events Topic을 구독
 * - 좋아요, 조회수 등 상품 관련 이벤트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

    private final EventInboxService eventInboxService;
    private final ProductMetricsService productMetricsService;
    private final RankingAggregator rankingAggregator;
    private final DeadLetterQueueService deadLetterQueueService;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 3;

    @KafkaListener(
        topics = "${kafka.topics.catalog-events}",
        groupId = "commerce-streamer-catalog",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeCatalogEvents(
        List<ConsumerRecord<Object, Object>> records,
        Acknowledgment acknowledgment
    ) {
        log.info("📦 Catalog 이벤트 수신 - count: {}", records.size());

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (ConsumerRecord<Object, Object> record : records) {
            try {
                boolean processed = processEvent(record);
                if (processed) {
                    successCount++;
                } else {
                    skipCount++;  // 중복 이벤트
                }
            } catch (Exception e) {
                failCount++;
                log.error("❌ 이벤트 처리 실패 - partition: {}, offset: {}, key: {}, error: {}",
                    record.partition(), record.offset(), record.key(), e.getMessage(), e);

                // DLQ에 전송
                sendToDLQ(record, e, 0);
            }
        }

        // Offset 커밋 (배치 단위)
        acknowledgment.acknowledge();

        log.info("✅ Catalog 이벤트 처리 완료 - success: {}, skip: {}, fail: {}, total: {}",
            successCount, skipCount, failCount, records.size());
    }

    /**
     * 이벤트 처리
     *
     * @return true: 처리 완료, false: 중복으로 스킵
     */
    @Transactional
    protected boolean processEvent(ConsumerRecord<Object, Object> record) throws Exception {
        Object value = record.value();
        String payload = (value instanceof String) ? (String) value : value.toString();
        Map<String, Object> eventData = objectMapper.readValue(payload, Map.class);

        // Outbox의 ID를 eventId로 사용
        String eventId = eventData.get("id") != null ? eventData.get("id").toString() : null;
        String eventType = (String) eventData.get("eventType");
        String aggregateType = (String) eventData.get("aggregateType");
        String aggregateId = (String) eventData.get("aggregateId");

        if (eventId == null) {
            log.warn("⚠️ eventId가 없는 메시지 - partition: {}, offset: {}",
                record.partition(), record.offset());
            return false;
        }

        // 1. 중복 체크 (Inbox)
        if (eventInboxService.isDuplicate(eventId)) {
            log.info("🔁 중복 이벤트 스킵 - eventId: {}, eventType: {}", eventId, eventType);
            return false;
        }

        // 2. Inbox 저장 (트랜잭션 시작)
        eventInboxService.save(eventId, aggregateType, aggregateId, eventType);

        // 3. 비즈니스 로직 처리
        Long productId = Long.parseLong(aggregateId);

        switch (eventType) {
            case "LikeCreatedEvent":
                productMetricsService.incrementLikeCount(productId);
                rankingAggregator.incrementLikeScore(productId);
                break;

            case "LikeDeletedEvent":
                productMetricsService.decrementLikeCount(productId);
                rankingAggregator.decrementLikeScore(productId);
                break;

            case "ProductViewedEvent":
                productMetricsService.incrementViewCount(productId);
                rankingAggregator.incrementViewScore(productId);
                break;

            default:
                log.warn("⚠️ 알 수 없는 이벤트 타입 - eventType: {}", eventType);
        }

        log.info("✨ 이벤트 처리 완료 - eventId: {}, eventType: {}, productId: {}",
            eventId, eventType, productId);

        return true;
    }

    /**
     * DLQ로 전송
     */
    private void sendToDLQ(ConsumerRecord<Object, Object> record, Exception error, int retryCount) {
        try {
            Object value = record.value();
            String payload = (value instanceof String) ? (String) value : value.toString();
            String eventId = extractEventId(payload);

            deadLetterQueueService.save(
                record.topic(),
                record.key() != null ? record.key().toString() : null,
                eventId,
                payload,
                error.getMessage(),
                retryCount
            );
        } catch (Exception e) {
            log.error("❌ DLQ 저장 실패 - error: {}", e.getMessage(), e);
        }
    }

    /**
     * payload에서 eventId 추출
     */
    private String extractEventId(String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            Object id = data.get("id");
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
