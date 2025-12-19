package com.loopers.event.consumer;

import com.loopers.event.EventIdempotencyService;
import com.loopers.shared.event.DomainEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 메시지 처리의 공통 관심사를 캡슐화하는 컴포넌트
 * - 이벤트 멱등성 처리 (EventIdempotencyService)
 * - 메트릭 기록 (MeterRegistry)
 * - Acknowledgment 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageProcessor {

    private final EventIdempotencyService eventIdempotencyService;
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
        T event = record.value();
        String metricName = metricPrefix + "." + event.getClass().getSimpleName().toLowerCase();

        log.debug("Processing event - topic: {}, type: {}, eventId: {}", 
                record.topic(), event.getClass().getSimpleName(), event.getEventId());

        // 중복 체크
        if (!eventIdempotencyService.tryAcquire(event.getEventId())) {
            log.info("Duplicate event detected - topic: {}, eventId: {}, skipping", 
                    record.topic(), event.getEventId());
            meterRegistry.counter("kafka.consumer.events", 
                    "type", metricName, 
                    "status", "skipped").increment();
            ack.acknowledge();
            return;
        }

        try {
            businessLogic.execute(event);
            meterRegistry.counter("kafka.consumer.events", 
                    "type", metricName, 
                    "status", "success").increment();
            // 성공 시에만 ack (At Least Once 보장)
            ack.acknowledge();
        } catch (Exception e) {
            // 처리 실패 → 멱등성 키를 삭제하여 재시도 가능하도록 함
            eventIdempotencyService.release(event.getEventId());
            log.error("Error processing event - topic: {}, eventId: {}, error: {}", 
                    record.topic(), event.getEventId(), e.getMessage(), e);
            meterRegistry.counter("kafka.consumer.events", 
                    "type", metricName, 
                    "status", "failure").increment();
            // 예외를 던져서 재시도 유도 (최대 재시도 횟수 초과 시 ErrorHandler가 DLQ에 저장)
            throw new RuntimeException("Failed to process event: " + e.getMessage(), e);
        }
    }

    /**
     * 비즈니스 로직을 표현하는 함수형 인터페이스
     */
    @FunctionalInterface
    public interface BusinessLogic<T extends DomainEvent> {
        void execute(T event);
    }
}

