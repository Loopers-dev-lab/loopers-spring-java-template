package com.loopers.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

/**
 * Outbox Event Cleaner
 * - 오래된 PUBLISHED 이벤트 정리
 * - 디스크 공간 절약 및 쿼리 성능 유지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventCleaner {

    private final OutboxEventRepository outboxRepository;

    /**
     * 7일 이상 지난 PUBLISHED 이벤트 삭제
     * - 매일 새벽 3시에 실행
     * - PUBLISHED 상태만 삭제 (PENDING, FAILED는 유지)
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    @Transactional
    public void cleanOldPublishedEvents() {
        ZonedDateTime cutoffDate = ZonedDateTime.now().minusDays(7);

        log.info("[OutboxCleaner] Starting cleanup of published events older than {}", cutoffDate);

        try {
            int deleted = outboxRepository.deletePublishedEventsOlderThan(
                OutboxEvent.OutboxStatus.PUBLISHED,
                cutoffDate
            );

            log.info("[OutboxCleaner] Deleted {} old published events", deleted);

        } catch (Exception e) {
            log.error("[OutboxCleaner] Failed to clean old published events", e);
        }
    }

    /**
     * 30일 이상 지난 FAILED 이벤트 삭제
     * - 매주 일요일 새벽 4시에 실행
     * - 재시도 횟수를 초과한 FAILED 이벤트는 30일 후 삭제
     */
    @Scheduled(cron = "0 0 4 * * SUN")  // 매주 일요일 새벽 4시
    @Transactional
    public void cleanOldFailedEvents() {
        ZonedDateTime cutoffDate = ZonedDateTime.now().minusDays(30);

        log.info("[OutboxCleaner] Starting cleanup of failed events older than {}", cutoffDate);

        try {
            int deleted = outboxRepository.deleteFailedEventsOlderThan(
                OutboxEvent.OutboxStatus.FAILED,
                cutoffDate,
                3  // 최대 재시도 횟수 초과한 이벤트만 삭제
            );

            log.info("[OutboxCleaner] Deleted {} old failed events", deleted);

        } catch (Exception e) {
            log.error("[OutboxCleaner] Failed to clean old failed events", e);
        }
    }

    /**
     * Outbox 테이블 통계 로깅
     * - 매시간 정각에 실행
     * - PENDING, PUBLISHED, FAILED 상태별 이벤트 개수 확인
     */
    @Scheduled(cron = "0 0 * * * *")  // 매시간 정각
    @Transactional(readOnly = true)
    public void logOutboxStatistics() {
        try {
            long pendingCount = outboxRepository.countByStatus(OutboxEvent.OutboxStatus.PENDING);
            long publishedCount = outboxRepository.countByStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            long failedCount = outboxRepository.countByStatus(OutboxEvent.OutboxStatus.FAILED);
            long totalCount = outboxRepository.count();

            log.info("[OutboxCleaner] Outbox statistics - Total: {}, PENDING: {}, PUBLISHED: {}, FAILED: {}",
                totalCount, pendingCount, publishedCount, failedCount);

            // PENDING 이벤트가 1000개 이상이면 경고
            if (pendingCount > 1000) {
                log.warn("[OutboxCleaner] WARNING: Too many PENDING events ({}). Check OutboxEventPoller!", pendingCount);
            }

            // FAILED 이벤트가 100개 이상이면 경고
            if (failedCount > 100) {
                log.warn("[OutboxCleaner] WARNING: Too many FAILED events ({}). Check Kafka connection!", failedCount);
            }

        } catch (Exception e) {
            log.error("[OutboxCleaner] Failed to log outbox statistics", e);
        }
    }
}
