package com.loopers.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.metrics.MetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메트릭 서비스의 메모리 락 정리 스케줄러
 * <p>
 * 주기적으로 사용하지 않는 락과 처리된 이벤트 캐시를 정리하여 메모리 누수를 방지합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsLockCleanupScheduler {

    private final MetricsService metricsService;

    /**
     * 사용하지 않는 락 정리 (5분마다)
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분
    public void cleanupUnusedLocks() {
        try {
            metricsService.cleanupUnusedLocks();
        } catch (Exception e) {
            log.error("락 정리 중 오류 발생", e);
        }
    }

    /**
     * 처리된 이벤트 캐시 정리 (10분마다)
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10분
    public void cleanupProcessedEvents() {
        try {
            metricsService.cleanupProcessedEvents();
        } catch (Exception e) {
            log.error("처리된 이벤트 캐시 정리 중 오류 발생", e);
        }
    }

    /**
     * 락 상태 모니터링 (1분마다)
     */
    @Scheduled(fixedRate = 60 * 1000) // 1분
    public void monitorLockStatus() {
        try {
            metricsService.logLockStatus();
        } catch (Exception e) {
            log.error("락 상태 모니터링 중 오류 발생", e);
        }
    }
}