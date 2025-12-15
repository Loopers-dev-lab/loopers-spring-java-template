package com.loopers.application.outbox;

import com.loopers.domain.outbox.EventOutbox;
import com.loopers.domain.outbox.EventOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 테이블을 폴링하여 이벤트를 발행하는 스케줄러
 * - PENDING 상태의 이벤트를 주기적으로 발행
 * - FAILED 상태의 이벤트를 재시도 (최대 3회)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final EventOutboxRepository outboxRepository;
    private final OutboxEventService outboxEventService;

    /**
     * PENDING 이벤트를 발행
     * - 5초마다 실행
     */
    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<EventOutbox> pendingEvents = outboxRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox에서 {} 개의 PENDING 이벤트 발행 시작", pendingEvents.size());

        for (EventOutbox event : pendingEvents) {
            try {
                outboxEventService.publishEvent(event);
            } catch (Exception e) {
                log.error("이벤트 발행 실패 - id: {}, type: {}, error: {}",
                    event.getId(), event.getEventType(), e.getMessage());
                // 예외는 OutboxEventService에서 이미 처리됨 (FAILED 상태로 변경)
            }
        }

        log.info("Outbox PENDING 이벤트 발행 완료");
    }

    /**
     * FAILED 이벤트를 재시도
     * - 30초마다 실행
     * - retryCount < 3인 이벤트만 재시도
     */
    @Scheduled(fixedDelay = 30000)
    public void retryFailedEvents() {
        List<EventOutbox> failedEvents = outboxRepository.findFailedEventsCanRetry();

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Outbox에서 {} 개의 FAILED 이벤트 재시도 시작", failedEvents.size());

        for (EventOutbox event : failedEvents) {
            try {
                outboxEventService.publishEvent(event);
                log.info("이벤트 재시도 성공 - id: {}, retryCount: {}",
                    event.getId(), event.getRetryCount());
            } catch (Exception e) {
                log.error("이벤트 재시도 실패 - id: {}, retryCount: {}, error: {}",
                    event.getId(), event.getRetryCount(), e.getMessage());

                // 재시도 횟수가 3회를 초과하면 경고
                if (event.getRetryCount() >= 3) {
                    log.error("⚠️ 이벤트 재시도 횟수 초과 - id: {}, type: {}, aggregateId: {}. 관리자 확인 필요!",
                        event.getId(), event.getEventType(), event.getAggregateId());
                }
            }
        }

        log.info("Outbox FAILED 이벤트 재시도 완료");
    }
}
