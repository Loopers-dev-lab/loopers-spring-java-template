package com.loopers.interfaces.scheduler;

import com.loopers.domain.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxService outboxService;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 3000)
    public void relayOutboxEvents() {
        try {
            log.debug("Outbox 이벤트 릴레이 시작 - batchSize: {}", BATCH_SIZE);
            outboxService.publishPendingOutboxes(BATCH_SIZE);
            log.debug("Outbox 이벤트 릴레이 완료");
        } catch (Exception e) {
            log.error("Outbox 이벤트 릴레이 중 오류 발생", e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void retryFailedOutboxEvents() {
        try {
            log.debug("실패한 Outbox 이벤트 재시도 시작 - batchSize: {}", BATCH_SIZE);
            outboxService.retryFailedOutboxes(BATCH_SIZE);
            log.debug("실패한 Outbox 이벤트 재시도 완료");
        } catch (Exception e) {
            log.error("실패한 Outbox 이벤트 재시도 중 오류 발생", e);
        }
    }
}

