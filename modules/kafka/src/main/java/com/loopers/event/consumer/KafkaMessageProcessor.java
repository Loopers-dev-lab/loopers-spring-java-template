package com.loopers.event.consumer;

import com.loopers.domain.event.InboxEventService;
import com.loopers.shared.event.DomainEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka 메시지 처리의 공통 관심사를 캡슐화하는 컴포넌트
 * - MessageId 생성
 * - 멱등성 처리 (InboxEventService)
 * - 메트릭 기록 (MeterRegistry)
 * - Acknowledgment 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageProcessor {

    private final InboxEventService inboxEventService;
    private final MeterRegistry meterRegistry;

    /**
     * DomainEvent를 처리합니다.
     * 
     * @param record Kafka ConsumerRecord
     * @param ack Acknowledgment
     * @param metricPrefix 메트릭 이름의 prefix (예: "stock", "order")
     * @param businessLogic 실제 비즈니스 로직
     * @param <T> DomainEvent를 구현한 이벤트 타입
     */
    public <T extends DomainEvent> void execute(
            ConsumerRecord<String, T> record,
            Acknowledgment ack,
            String metricPrefix,
            BusinessLogic<T> businessLogic
    ) {
        String messageId = generateMessageId(record);
        T event = record.value();
        LocalDateTime eventTimestamp = extractEventTimestamp(event);
        String metricName = metricPrefix + "." + event.getClass().getSimpleName().toLowerCase();

        log.debug("Processing event - topic: {}, messageId: {}, type: {}", 
                record.topic(), messageId, event.getClass().getSimpleName());

        try {
            inboxEventService.process(messageId, eventTimestamp, () -> {
                businessLogic.execute(event);
            });
            meterRegistry.counter("kafka.consumer.events", 
                    "type", metricName, 
                    "status", "success").increment();
        } catch (IllegalStateException e) {
            // 멱등성 처리: 이미 처리된 이벤트
            log.info("Event processing skipped for {}: {}", messageId, e.getMessage());
            meterRegistry.counter("kafka.consumer.events", 
                    "type", metricName, 
                    "status", "skipped").increment();
        } catch (Exception e) {
            // 처리 실패 → At Most Once를 위해 ack하고 로깅만 (재시도 안 함)
            log.error("Error processing event {}: {}", messageId, e.getMessage(), e);
            meterRegistry.counter("kafka.consumer.events", 
                    "type", metricName, 
                    "status", "failure").increment();
            // throw 하지 않음 → 재시도 안 함
        } finally {
            ack.acknowledge();  // 항상 ack (At Most Once 보장)
        }
    }

    /**
     * ConsumerRecord로부터 고유한 메시지 ID를 생성합니다.
     * 형식: {topic}-{partition}-{offset}
     */
    private String generateMessageId(ConsumerRecord<?, ?> record) {
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    /**
     * DomainEvent에서 발생 시각을 추출합니다.
     * 이벤트에 occurredAt이 없으면 현재 시각을 반환합니다.
     */
    private LocalDateTime extractEventTimestamp(DomainEvent event) {
        if (event != null && event.getOccurredAt() != null) {
            return event.getOccurredAt();
        }
        return LocalDateTime.now();
    }

    /**
     * 비즈니스 로직을 표현하는 함수형 인터페이스
     */
    @FunctionalInterface
    public interface BusinessLogic<T extends DomainEvent> {
        void execute(T event);
    }
}

