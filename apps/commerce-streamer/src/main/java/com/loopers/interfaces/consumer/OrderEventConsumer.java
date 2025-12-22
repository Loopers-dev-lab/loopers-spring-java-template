package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.dlq.DeadLetterQueueService;
import com.loopers.application.inbox.EventInboxService;
import com.loopers.application.metrics.ProductMetricsService;
import com.loopers.application.ranking.RankingAggregator;
import com.loopers.confg.kafka.KafkaConfig;
import java.math.BigDecimal;
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
 * Order 이벤트 Consumer
 * - order-events Topic을 구독
 * - 주문, 결제 관련 이벤트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EventInboxService eventInboxService;
    private final ProductMetricsService productMetricsService;
    private final RankingAggregator rankingAggregator;
    private final DeadLetterQueueService deadLetterQueueService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.order-events}",
        groupId = "commerce-streamer-order",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeOrderEvents(
        List<ConsumerRecord<Object, Object>> records,
        Acknowledgment acknowledgment
    ) {
        log.info("📦 Order 이벤트 수신 - count: {}", records.size());

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (ConsumerRecord<Object, Object> record : records) {
            try {
                boolean processed = processEvent(record);
                if (processed) {
                    successCount++;
                } else {
                    skipCount++;
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

        log.info("✅ Order 이벤트 처리 완료 - success: {}, skip: {}, fail: {}, total: {}",
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

        // 2. Inbox 저장
        eventInboxService.save(eventId, aggregateType, aggregateId, eventType);

        // 3. 비즈니스 로직 처리
        if ("OrderCreatedEvent".equals(eventType)) {
            processOrderCreated(payload);
        } else if ("PaymentSuccessEvent".equals(eventType)) {
            // 추가 처리 로직 (필요 시)
            log.info("💰 결제 성공 이벤트 수신 - eventId: {}", eventId);
        }

        log.info("✨ 이벤트 처리 완료 - eventId: {}, eventType: {}",
            eventId, eventType);

        return true;
    }

    /**
     * OrderCreatedEvent 처리
     * - 주문 수, 판매 금액 증가
     */
    private void processOrderCreated(String payload) throws Exception {
        Map<String, Object> eventData = objectMapper.readValue(payload, Map.class);

        // 실제 이벤트 구조에 맞춰 파싱 (예시)
        // OrderCreatedEvent의 실제 필드명에 맞춰 수정 필요
        Object payloadObj = eventData.get("payload");

        if (payloadObj == null) {
            log.warn("⚠️ payload가 없는 OrderCreatedEvent");
            return;
        }

        // payload가 String이면 다시 파싱
        Map<String, Object> orderData;
        if (payloadObj instanceof String) {
            orderData = objectMapper.readValue((String) payloadObj, Map.class);
        } else {
            orderData = (Map<String, Object>) payloadObj;
        }

        // 필드 추출 (실제 OrderCreatedEvent 구조에 맞춰 수정 필요)
        Object productIdObj = orderData.get("productId");
        Object quantityObj = orderData.get("quantity");
        Object amountObj = orderData.get("totalAmount");

        if (productIdObj != null && quantityObj != null && amountObj != null) {
            Long productId = Long.parseLong(productIdObj.toString());
            Integer quantity = Integer.parseInt(quantityObj.toString());
            BigDecimal amount = new BigDecimal(amountObj.toString());

            productMetricsService.incrementOrderCount(productId, quantity, amount);

            // 랭킹 점수 반영 (단가 계산)
            int unitPrice = amount.divide(new BigDecimal(quantity), 0, BigDecimal.ROUND_HALF_UP).intValue();
            rankingAggregator.incrementOrderScore(productId, unitPrice, quantity);
        } else {
            log.warn("⚠️ OrderCreatedEvent에 필수 필드 누락 - productId: {}, quantity: {}, amount: {}",
                productIdObj, quantityObj, amountObj);
        }
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
