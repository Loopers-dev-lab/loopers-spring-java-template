package com.loopers.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventService;
import com.loopers.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventService outboxEventService;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final int QUERY_LIMIT = 200;

    @Scheduled(fixedDelay = 3000)  // 3초마다 실행하여 이벤트 발행 처리
    public void publishPendingEvents() {
        // 이벤트 발행 상태가 PENDING 인 목록을 가져온다.
        List<OutboxEvent> pendingEvents = outboxEventService.getPendingEvents(QUERY_LIMIT);

        for(OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Outbox 이벤트 발행 실패 - id: {}", event.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(OutboxEvent outboxEvent) {
        try {
            // 토픽 결정 (aggregateType 기반)
            String topic = determineTopicByAggregateType(outboxEvent.getAggregateType());

            // 전체 메시지 구조 생성 (Consumer가 기대하는 형식)
            String message = createEventMessage(outboxEvent);

            // Kafka 발행
            kafkaTemplate.send(
                    topic,
                    outboxEvent.getAggregateId(),  // Partition Key
                    message  // JSON String으로 전송
            ).get(10, TimeUnit.SECONDS);  // 타임아웃 설정

            // 발행 성공 시 상태 업데이트
            outboxEvent.markAsPublished();
            outboxEventService.save(outboxEvent);

            log.info("Outbox 이벤트 발행 완료 - id: {}, aggregateType: {}, eventType: {}",
                    outboxEvent.getId(), outboxEvent.getAggregateType(), outboxEvent.getEventType());

        } catch (Exception e) {
            log.error("Kafka 발행 실패 - Outbox id: {}", outboxEvent.getId(), e);
            outboxEvent.markAsFailed();
            outboxEventService.save(outboxEvent);
        }
    }

    private String createEventMessage(OutboxEvent outboxEvent) throws Exception {
        // payload를 Object로 파싱
        Object payloadObject = objectMapper.readValue(outboxEvent.getPayload(), Object.class);

        // 전체 메시지 구조 생성
        var message = java.util.Map.of(
                "eventId", outboxEvent.getId().toString(),
                "eventType", outboxEvent.getEventType(),
                "aggregateType", outboxEvent.getAggregateType(),
                "aggregateId", outboxEvent.getAggregateId(),
                "payload", payloadObject
        );

        return objectMapper.writeValueAsString(message);
    }

    private String determineTopicByAggregateType(String aggregateType) {
        return switch (aggregateType) {
            case "PRODUCT_LIKE" -> KafkaTopics.PRODUCT_LIKE;
            case "ORDER" -> KafkaTopics.ORDER;
            case "COUPON" -> KafkaTopics.COUPON;
            case "PRODUCT_VIEW" -> KafkaTopics.PRODUCT;
            case "ACTIVITY" -> KafkaTopics.USER_ACTIVITY;
            default -> throw new IllegalArgumentException("Unknown aggregate type: " + aggregateType);
        };
    }
}
