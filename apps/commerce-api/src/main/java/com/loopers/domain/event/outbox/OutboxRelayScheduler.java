package com.loopers.domain.event.outbox;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Outbox 이벤트 배치 처리 스케줄러
 * <p>
 * 순수 Outbox 패턴:
 * - 모든 이벤트는 Outbox에 READY 상태로 저장
 * - 이 스케줄러가 배치 단위로 Kafka 발송 담당
 * - 실패한 이벤트는 재시도 처리
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
     * READY 상태 이벤트 배치 처리 (1초마다)
     * 순수 Outbox 패턴 - 모든 이벤트를 스케줄러가 처리
     */
    @Scheduled(fixedDelay = 1000)
    public void processReadyEvents() {
        final List<OutboxEventEntity> readyEvents =
                outboxService.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.READY);

        if (readyEvents.isEmpty()) {
            return; // 로그 없이 조용히 리턴
        }

        log.info("READY 이벤트 배치 처리 시작 - 처리할 이벤트 수: {}", readyEvents.size());

        int successCount = 0;
        int failCount = 0;

        for (OutboxEventEntity event : readyEvents) {
            boolean isSuccess = outboxService.processEvent(event);
            if (isSuccess) {
                successCount++;
                log.debug("이벤트 발송 성공 - eventId: {}, type: {}", 
                         event.getEventId(), event.getEventType());
            } else {
                failCount++;
            }
        }

        log.info("READY 이벤트 배치 처리 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 실패한 이벤트 재시도 (FAILED 상태)
     * 30초마다 실행 - 실패한 이벤트만 재시도
     */
    @Scheduled(fixedDelay = 30000)
    public void retryFailedEvents() {
        final List<OutboxEventEntity> failedEvents =
                outboxService.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                        OutboxStatus.FAILED, MAX_RETRY_COUNT);

        if (failedEvents.isEmpty()) {
            return; // 로그 없이 조용히 리턴
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
