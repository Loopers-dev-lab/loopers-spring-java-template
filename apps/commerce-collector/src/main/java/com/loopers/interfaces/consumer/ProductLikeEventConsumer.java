package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.like.ProductLikeEventHandler;
import com.loopers.kafka.KafkaTopics;
import com.loopers.kafka.KafkaTopics.ProductLike;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeEventConsumer {
    private final ProductLikeEventHandler productLikeEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.PRODUCT_LIKE,
            groupId = "commerce-collector-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProductLikeEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("좋아요 이벤트 수신 - key: {}, message: {}", key, message);

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(message);

            // 필수 필드 검증
            if (!jsonNode.has("eventId") || !jsonNode.has("eventType") || !jsonNode.has("payload")) {
                log.error("잘못된 메시지 형식 - 필수 필드 누락: {}", message);
                acknowledgment.acknowledge();  // 재시도 방지
                return;
            }

            String eventId = jsonNode.get("eventId").asText();
            String eventType = jsonNode.get("eventType").asText();
            JsonNode payload = jsonNode.get("payload");

            // 이벤트 타입별 처리
            if (ProductLike.LIKE_ADDED.equals(eventType)) {
                // 이벤트별 필드 검증
                if (!payload.has("productId")) {
                    log.error("잘못된 LIKE_ADDED 형식 - eventId: {}, payload: {}", eventId, payload);
                    acknowledgment.acknowledge();  // 재시도 방지
                    return;
                }

                Long productId = payload.get("productId").asLong();
                productLikeEventHandler.handleLikeAdded(eventId, productId);
            } else if (ProductLike.LIKE_REMOVED.equals(eventType)) {
                // 이벤트별 필드 검증
                if (!payload.has("productId")) {
                    log.error("잘못된 LIKE_REMOVED 형식 - eventId: {}, payload: {}", eventId, payload);
                    acknowledgment.acknowledge();  // 재시도 방지
                    return;
                }

                Long productId = payload.get("productId").asLong();
                productLikeEventHandler.handleLikeRemoved(eventId, productId);
            }

            // 수동 커밋
            acknowledgment.acknowledge();
            log.info("이벤트 처리 완료 - eventId: {}", eventId);

        } catch (JsonProcessingException e) {
            // JSON 파싱 에러 - 재시도 불필요
            log.error("JSON 파싱 실패 (재시도 안 함) - message: {}", message, e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Business 로직 에러 - 재시도
            log.error("이벤트 처리 실패 (재시도) - message: {}", message, e);
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }
}
