package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 1초마다 Pending 상태의 Outbox 이벤트를 Kafka로 발행
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(BATCH_SIZE);

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getPartitionKey(),
                        event.getPayload()
                ).get();  // 동기적으로 전송 확인

                event.markAsProcessed();
                outboxEventRepository.save(event);

                log.debug("Outbox 이벤트 발행 성공: id={}, topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                event.markAsFailed(e.getMessage());
                outboxEventRepository.save(event);
                log.error("Outbox 이벤트 발행 실패: id={}, topic={}", event.getId(), event.getTopic(), e);
            }
        }
    }

    /**
     * 5분마다 실패한 이벤트 재시도
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findFailedEventsForRetry(MAX_RETRY_COUNT, BATCH_SIZE);

        for (OutboxEvent event : failedEvents) {
            event.markForRetry();
            outboxEventRepository.save(event);
            log.info("실패한 Outbox 이벤트 재시도 대기열로 이동: id={}", event.getId());
        }
    }
}
