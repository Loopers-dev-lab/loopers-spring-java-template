package com.loopers.domain.event.outbox;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실패한 Outbox 이벤트 재시도 스케줄러
 * <p>
 * Event-Driven + Outbox Fallback 패턴에서:
 * - 즉시 발송은 이벤트 핸들러에서 담당
 * - 실패한 이벤트만 이 스케줄러에서 재시도
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxService outboxService;
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 실패한 이벤트만 재시도 (FAILED 상태)
     * 30초마다 실행 - 즉시 발송이 주 방식이므로 재시도는 여유롭게
     */
    @Scheduled(fixedDelay = 30000)
    public void retryFailedEvents() {
        final List<OutboxEventEntity> failedEvents =
                outboxService.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                        OutboxStatus.FAILED, MAX_RETRY_COUNT);

        if (failedEvents.isEmpty()) {
            log.debug("재시도할 실패 이벤트 없음");
            return;
        }

        log.info("실패 이벤트 재시도 시작 - 처리할 이벤트 수: {}", failedEvents.size());

        int retrySuccessCount = 0;
        int retryFailCount = 0;
        int maxRetryReachedCount = 0;

        for (OutboxEventEntity event : failedEvents) {
            if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                maxRetryReachedCount++;
                log.warn("최대 재시도 횟수 초과 - eventId: {}, retryCount: {}",
                        event.getEventId(), event.getRetryCount());
                continue;
            }

            boolean isSuccess = outboxService.processEvent(event);
            if (isSuccess) {
                retrySuccessCount++;
                log.info("재시도 성공 - eventId: {}, retryCount: {}",
                        event.getEventId(), event.getRetryCount());
            } else {
                retryFailCount++;
            }
        }

        log.info("실패 이벤트 재시도 완료 - 성공: {}, 실패: {}, 최대재시도초과: {}",
                retrySuccessCount, retryFailCount, maxRetryReachedCount);
    }
}
