package com.loopers.application.metrics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

import com.loopers.domain.event.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import com.loopers.infrastructure.cache.ProductCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메트릭 Application Service
 * <p>
 * 메트릭 관련 유스케이스를 조합하는 Application 계층 서비스입니다.
 * EventProcessingFacade에서 호출되어 메트릭 처리를 담당합니다.
 * <p>
 * 책임:
 * - 멱등성 체크 (메모리 캐시 + Domain Service 위임)
 * - 동시성 제어 (상품별 메모리 락)
 * - 메트릭 업데이트 조정 (Domain Service 위임)
 * - 캐시 무효화 조정 (Infrastructure 위임)
 * <p>
 * 의존관계:
 * - ProductMetricsService (Domain) - 메트릭 비즈니스 로직
 * - EventHandledService (Domain) - 멱등성 처리
 * - ProductCacheService (Infrastructure) - 캐시 처리
 *
 * @author hyunjikoh
 * @since 2025. 12. 26.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    // Domain Layer 의존성
    private final ProductMetricsService productMetricsService;
    private final EventHandledService eventHandledService;

    // Infrastructure Layer 의존성
    private final ProductCacheService productCacheService;

    // 상품별 메모리 락 관리
    private final ConcurrentHashMap<Long, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    // 설정값
    private static final long LOCK_WAIT_TIME_MS = 100;
    private static final int LOCK_CLEANUP_THRESHOLD = 10000;

    // 메모리 기반 멱등성 캐시 (빠른 경로)
    private final ConcurrentHashMap<String, Boolean> processedEventsCache = new ConcurrentHashMap<>();
    private static final int PROCESSED_EVENTS_CLEANUP_THRESHOLD = 50000;

    // ========== 멱등성 체크 ==========

    /**
     * 이벤트 처리 여부 확인 및 마킹
     *
     * @param eventId 이벤트 ID
     * @return true: 처음 처리, false: 이미 처리됨
     */
    public boolean tryMarkHandled(String eventId) {
        // 1. 메모리 캐시 먼저 확인 (빠른 경로)
        if (processedEventsCache.containsKey(eventId)) {
            log.debug("이미 처리된 이벤트 (메모리 캐시): {}", eventId);
            return false;
        }

        // 2. Domain Service를 통해 DB 확인
        if (eventHandledService.isAlreadyHandled(eventId)) {
            processedEventsCache.put(eventId, true);
            log.debug("이미 처리된 이벤트 (DB 확인): {}", eventId);
            return false;
        }

        // 3. Domain Service를 통해 새로운 이벤트 저장
        boolean saved = eventHandledService.markAsHandled(eventId);
        if (saved) {
            processedEventsCache.put(eventId, true);
            return true;
        } else {
            processedEventsCache.put(eventId, true);
            log.debug("동시성으로 인한 중복 이벤트: {}", eventId);
            return false;
        }
    }

    // ========== 메트릭 업데이트 ==========

    /**
     * 조회수 증가
     */
    public void incrementView(Long productId, long occurredAtEpochMillis) {
        executeWithLock(productId, () -> {
            ZonedDateTime eventTime = convertToZonedDateTime(occurredAtEpochMillis);
            productMetricsService.incrementView(productId, eventTime);
            log.debug("조회수 업데이트 성공: productId={}", productId);
        });
    }

    /**
     * 좋아요 수 변경
     */
    public void applyLikeDelta(Long productId, int delta, long occurredAtEpochMillis) {
        executeWithLock(productId, () -> {
            ZonedDateTime eventTime = convertToZonedDateTime(occurredAtEpochMillis);
            productMetricsService.applyLikeDelta(productId, delta, eventTime);
            log.debug("좋아요 수 업데이트 성공: productId={}, delta={}", productId, delta);
        });
    }

    /**
     * 판매량 증가
     */
    public void addSales(Long productId, int quantity, java.math.BigDecimal totalAmount, long occurredAtEpochMillis) {
        executeWithLock(productId, () -> {
            ZonedDateTime eventTime = convertToZonedDateTime(occurredAtEpochMillis);
            boolean updated = productMetricsService.addSales(productId, quantity, totalAmount, eventTime);

            if (updated) {
                // 캐시 무효화 (판매량 변경 - 인기 상품 순위 영향)
                productCacheService.onSalesCountChanged(productId);
                log.debug("판매량 업데이트 성공: productId={}, quantity={}, totalAmount={}", productId, quantity, totalAmount);
            }
        });
    }

    /**
     * 재고 소진 이벤트 처리
     */
    public void handleStockDepleted(Long productId, Long brandId, Integer remainingStock, long occurredAtEpochMillis) {
        int stockToUpdate = (remainingStock != null) ? remainingStock : 0;
        productCacheService.updateProductStock(productId, stockToUpdate);
        log.info("재고 소진 캐시 갱신 완료: productId={}, brandId={}, remainingStock={}",
                productId, brandId, stockToUpdate);
    }

    // ========== Helper Methods ==========

    private void executeWithLock(Long productId, Runnable action) {
        ReentrantLock lock = productLocks.computeIfAbsent(productId, k -> new ReentrantLock());

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    action.run();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("메트릭 업데이트 스킵 - 락 획득 실패: productId={}", productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("메트릭 업데이트 중단 - 스레드 인터럽트: productId={}", productId);
        }
    }

    private ZonedDateTime convertToZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    // ========== 모니터링 및 정리 ==========

    public void cleanupUnusedLocks() {
        if (productLocks.size() > LOCK_CLEANUP_THRESHOLD) {
            log.info("락 정리 시작 - 현재 락 수: {}", productLocks.size());
            productLocks.entrySet().removeIf(entry -> {
                ReentrantLock lock = entry.getValue();
                return !lock.isLocked() && !lock.hasQueuedThreads();
            });
            log.info("락 정리 완료 - 정리 후 락 수: {}", productLocks.size());
        }
    }

    public void cleanupProcessedEvents() {
        if (processedEventsCache.size() > PROCESSED_EVENTS_CLEANUP_THRESHOLD) {
            log.info("처리된 이벤트 캐시 정리 시작 - 현재 캐시 수: {}", processedEventsCache.size());
            int targetSize = PROCESSED_EVENTS_CLEANUP_THRESHOLD / 2;
            int toRemove = processedEventsCache.size() - targetSize;
            processedEventsCache.entrySet().stream()
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .forEach(processedEventsCache::remove);
            log.info("처리된 이벤트 캐시 정리 완료 - 정리 후 캐시 수: {}", processedEventsCache.size());
        }
    }

    public void logLockStatus() {
        int totalLocks = productLocks.size();
        long lockedCount = productLocks.values().stream()
                .filter(ReentrantLock::isLocked)
                .count();
        if (totalLocks > 0) {
            log.debug("메트릭 락 상태 - 총 락: {}, 사용 중: {}", totalLocks, lockedCount);
        }
    }
}
