package com.loopers.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.kafka.EventKafkaProducer;
import com.loopers.domain.outbox.EventOutbox;
import com.loopers.domain.outbox.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 패턴을 사용한 이벤트 발행 서비스
 * - 이벤트를 DB에 먼저 저장
 * - 별도 프로세스가 읽어서 실제 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final EventOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EventKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    /**
     * 이벤트를 Outbox에 저장
     * - 비즈니스 트랜잭션과 같은 트랜잭션에서 실행됨
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId,
        String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            EventOutbox outbox = EventOutbox.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .build();

            outboxRepository.save(outbox);

            log.info("Outbox에 이벤트 저장 - type: {}, aggregateId: {}",
                eventType, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패 - type: {}, error: {}",
                eventType, e.getMessage(), e);
            throw new RuntimeException("이벤트 저장 실패", e);
        }
    }

    /**
     * Outbox에서 이벤트를 읽어 Kafka로 발행
     * - 별도 트랜잭션에서 실행
     * - Kafka 발행 성공 시 Outbox 상태를 PUBLISHED로 변경
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(EventOutbox outbox) {
        try {
            // Kafka로 발행
            kafkaProducer.publish(outbox)
                .thenAccept(result -> {
                    // 발행 성공 시 Outbox 상태 업데이트
                    outbox.markAsPublished();
                    outboxRepository.save(outbox);

                    log.info("Outbox → Kafka 발행 완료 - outboxId: {}, eventType: {}, offset: {}",
                        outbox.getId(),
                        outbox.getEventType(),
                        result.getRecordMetadata().offset());
                })
                .exceptionally(ex -> {
                    // 발행 실패 시 Outbox 상태 업데이트
                    outbox.markAsFailed(ex.getMessage());
                    outboxRepository.save(outbox);

                    log.error("Outbox → Kafka 발행 실패 - outboxId: {}, error: {}",
                        outbox.getId(), ex.getMessage(), ex);

                    return null;
                })
                .join();  // 동기 대기 (트랜잭션 내에서 완료 보장)

        } catch (Exception e) {
            log.error("Outbox 이벤트 발행 실패 - id: {}, error: {}",
                outbox.getId(), e.getMessage(), e);

            outbox.markAsFailed(e.getMessage());
            outboxRepository.save(outbox);

            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }

    private Object deserializeEvent(EventOutbox outbox) throws JsonProcessingException {
        Class<?> eventClass = getEventClass(outbox.getEventType());
        return objectMapper.readValue(outbox.getPayload(), eventClass);
    }

    private Class<?> getEventClass(String eventType) {
        try {
            return Class.forName("com.loopers.domain." +
                getPackageName(eventType) + ".event." + eventType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("이벤트 클래스를 찾을 수 없습니다: " + eventType, e);
        }
    }

    private String getPackageName(String eventType) {
        if (eventType.startsWith("Order")) {
            return "order";
        } else if (eventType.startsWith("Payment")) {
            return "payment";
        } else if (eventType.startsWith("Like")) {
            return "like";
        }
        throw new IllegalArgumentException("알 수 없는 이벤트 타입: " + eventType);
    }
}
