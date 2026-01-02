package com.loopers.interfaces.consumer.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductBatchEventHandler;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.infrastructure.kafka.DeadLetterQueuePublisher;
import com.loopers.interfaces.consumer.product.dto.ProductEvent;
import com.loopers.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBatchEventConsumer {

    private final ProductBatchEventHandler productBatchEventHandler;
    private final ObjectMapper objectMapper;
    private final DeadLetterQueuePublisher dlqPublisher;

    @KafkaListener(
            topics = KafkaTopics.PRODUCT,
            groupId = "commerce-collector-batch-group",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeProductViewBatch(
            @Payload List<String> messages,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment
    ) {
        int parsedCount = 0;
        int dlqCount = 0;

        try {
            log.info("상품 조회 이벤트 배치 처리 시작 - 메시지 수: {}", messages.size());

            // 1. JSON 파싱 (개별 메시지 에러 핸들링)
            List<ProductEvent> validEvents = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                try {
                    ProductEvent event = parseEvent(messages.get(i));
                    if (event != null) {
                        validEvents.add(event);
                        parsedCount++;
                    } else {
                        // parseEvent에서 null 반환 (검증 실패) → DLQ
                        dlqPublisher.sendRawMessageToDLQ(
                                KafkaTopics.PRODUCT_DLQ,
                                messages.get(i),
                                keys.get(i),
                                offsets.get(i),
                                new IllegalArgumentException("메시지 검증 실패")
                        );
                        dlqCount++;
                    }
                } catch (Exception e) {
                    // 파싱 실패 → DLQ
                    dlqPublisher.sendRawMessageToDLQ(
                            KafkaTopics.PRODUCT_DLQ,
                            messages.get(i),
                            keys.get(i),
                            offsets.get(i),
                            e
                    );
                    dlqCount++;
                }
            }

            // 2. 배치 처리 (Handler에서 조회수 집계)
            if (!validEvents.isEmpty()) {
                try {
                    productBatchEventHandler.handleProductViewBatch(validEvents);
                } catch (Exception e) {
                    // 배치 처리 실패 → 개별 메시지로 재시도
                    log.warn("배치 처리 실패, 개별 메시지로 재시도 - 이벤트 수: {}", validEvents.size());
                    dlqCount += handleFailedBatch(validEvents, e);
                }
            }

            // 3. 수동 커밋 (성공/실패 관계없이 커밋 - DLQ로 보냈으므로)
            acknowledgment.acknowledge();
            log.info("배치 처리 완료 - 전체: {}, 성공: {}, DLQ: {}",
                    messages.size(), parsedCount - dlqCount, dlqCount);

        } catch (Exception e) {
            // 예상치 못한 오류 → 전체 배치 재시도
            log.error("배치 처리 중 예기치 않은 오류 (재시도)", e);
            throw new RuntimeException("배치 처리 실패", e);
        }
    }

    /**
     * 배치 처리 실패 시 개별 메시지로 재시도
     * @return DLQ로 보낸 메시지 수
     */
    private int handleFailedBatch(List<ProductEvent> events, Exception batchError) {
        int dlqCount = 0;

        for (ProductEvent event : events) {
            try {
                // 개별 메시지로 재처리 시도
                productBatchEventHandler.handleProductViewBatch(List.of(event));
            } catch (Exception e) {
                // 개별 처리도 실패 → DLQ
                log.error("개별 메시지 처리 실패 - eventId: {}", event.eventId(), e);
                dlqPublisher.sendParsedEventToDLQ(
                        KafkaTopics.PRODUCT_DLQ,
                        event,
                        event.eventId(),
                        e
                );
                dlqCount++;
            }
        }

        return dlqCount;
    }

    private ProductEvent parseEvent(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            // 필수 필드 검증
            if (!jsonNode.has("eventId") || !jsonNode.has("eventType") || !jsonNode.has("payload")) {
                log.warn("잘못된 메시지 형식 (스킵): {}", message);
                return null;
            }

            String eventId = jsonNode.get("eventId").asText();
            String eventType = jsonNode.get("eventType").asText();
            JsonNode payload = jsonNode.get("payload");

            if (!payload.has("productId")) {
                log.warn("productId 누락 (스킵): {}", message);
                return null;
            }

            Long productId = payload.get("productId").asLong();

            return new ProductEvent(
                    eventId,
                    eventType,
                    productId
            );

        } catch (JsonProcessingException e) {
            log.warn("메시지 파싱 실패: {}", message);
            return null;
        }
    }
}
