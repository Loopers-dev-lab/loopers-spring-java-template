package com.loopers.application.kafka;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxAfterCommitListener {

    private final KafkaEventPublisher kafkaEventPublisher;
    private final OutboxEventRepository outboxEventRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OutboxSavedEvent event) {
        try {
            log.info("Publishing event after commit: {}", event.outboxId());
            
            OutboxEvent outboxEvent = outboxEventRepository.findById(event.outboxId())
                .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + event.outboxId()));
            
            kafkaEventPublisher.publishEvent(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to publish event after commit: {}", event.outboxId(), e);
            // 실패 시에도 별도 처리하지 않고 OutboxEventReplayService가 재처리하도록 함
        }
    }
}