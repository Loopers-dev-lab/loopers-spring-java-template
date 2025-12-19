package com.loopers.domain.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import com.loopers.domain.event.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ConcurrentHashMap 기반 동시성 안전한 메트릭 서비스
 * <p>
 * 상품별 메모리 락을 사용하여 동일한 상품에 대한 동시 업데이트를 제어합니다.
 * Redis 분산락 대신 메모리 기반 락을 사용하여 성능을 대폭 향상시킵니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsService {
    private final EventRepository eventHandledRepository;
    private final ProductMetricsService metricsTransactionService;

    // 상품별 메모리 락 관리
    private final ConcurrentHashMap<Long, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    // 락 획득 설정 (빠른 처리를 위해 짧게 설정)
    private static final long LOCK_WAIT_TIME_MS = 100; // 100ms 대기
    private static final int LOCK_CLEANUP_THRESHOLD = 10000; // 락 정리 임계값

    // 메모리 기반 멱등성 체크 (성능 최적화)
    private final ConcurrentHashMap<String, Boolean> processedEvents = new ConcurrentHashMap<>();
    private static final int PROCESSED_EVENTS_CLEANUP_THRESHOLD = 50000; // 처리된 이벤트 정리 임계값

    /**
     * 멱등성 체크 - 메모리 기반으로 성능 최적화
     * 예외 기반이 아닌 조회 기반으로 중복 체크를 수행하여 성능을 향상시킵니다.
     */
    public boolean tryMarkHandled(String eventId) {
        // 1. 메모리 캐시 먼저 확인 (빠른 경로)
        if (processedEvents.containsKey(eventId)) {
            log.debug("이미 처리된 이벤트 (메모리 캐시): {}", eventId);
            return false;
        }

        // 2. DB에서 확인 (느린 경로)
        if (eventHandledRepository.existsById(eventId)) {
            // DB에 있으면 메모리 캐시에도 추가
            processedEvents.put(eventId, true);
            log.debug("이미 처리된 이벤트 (DB 확인): {}", eventId);
            return false;
        }

        // 3. 새로운 이벤트 - 트랜잭션 서비스를 통해 안전하게 저장
        boolean saved = metricsTransactionService.saveEventHandled(eventId);
        if (saved) {
            processedEvents.put(eventId, true);
            return true;
        } else {
            // 동시성으로 인해 다른 스레드가 먼저 저장한 경우
            processedEvents.put(eventId, true);
            log.debug("동시성으로 인한 중복 이벤트: {}", eventId);
            return false;
        }
    }

    /**
     * 조회수 증가 (메모리 락 적용)
     */
    public void incrementView(Long productId, long occurredAtEpochMillis) {
        ReentrantLock lock = getProductLock(productId);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    metricsTransactionService.incrementViewWithTransaction(productId, occurredAtEpochMillis);
                    log.debug("조회수 업데이트 성공: productId={}", productId);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("조회수 업데이트 스킵 - 락 획득 실패: productId={}", productId);
                // 락 획득 실패 시 이벤트 스킵 (성능 우선)
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("조회수 업데이트 중단 - 스레드 인터럽트: productId={}", productId);
        }
    }



    /**
     * 좋아요 수 변경 (메모리 락 적용)
     */
    public void applyLikeDelta(final Long productId, final int delta, long occurredAtEpochMillis) {
        ReentrantLock lock = getProductLock(productId);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    metricsTransactionService.applyLikeDeltaWithTransaction(productId, delta, occurredAtEpochMillis);
                    log.debug("좋아요 수 업데이트 성공: productId={}, delta={}", productId, delta);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("좋아요 수 업데이트 스킵 - 락 획득 실패: productId={}, delta={}", productId, delta);
                // 락 획득 실패 시 이벤트 스킵 (성능 우선)
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("좋아요 수 업데이트 중단 - 스레드 인터럽트: productId={}, delta={}", productId, delta);
        }
    }



    /**
     * 판매량 증가 (메모리 락 적용)
     */
    public void addSales(final Long productId, final int quantity, long occurredAtEpochMillis) {
        ReentrantLock lock = getProductLock(productId);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    metricsTransactionService.addSalesWithTransaction(productId, quantity, occurredAtEpochMillis);
                    log.debug("판매량 업데이트 성공: productId={}, quantity={}", productId, quantity);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("판매량 업데이트 스킵 - 락 획득 실패: productId={}, quantity={}", productId, quantity);

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("판매량 업데이트 중단 - 스레드 인터럽트: productId={}, quantity={}", productId, quantity);
        }
    }



    /**
     * 장바구니 추가 이벤트 처리 (메모리 락 적용)
     */
    public void incrementCartAdd(Long productId, long occurredAtEpochMillis) {
        ReentrantLock lock = getProductLock(productId);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    metricsTransactionService.incrementCartAddWithTransaction(productId, occurredAtEpochMillis);
                    log.debug("장바구니 추가 업데이트 성공: productId={}", productId);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("장바구니 추가 업데이트 스킵 - 락 획득 실패: productId={}", productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("장바구니 추가 업데이트 중단 - 스레드 인터럽트: productId={}", productId);
        }
    }

    /**
     * 위시리스트 추가 이벤트 처리 (메모리 락 적용)
     */
    public void incrementWishlistAdd(Long productId, long occurredAtEpochMillis) {
        ReentrantLock lock = getProductLock(productId);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    metricsTransactionService.incrementWishlistAddWithTransaction(productId, occurredAtEpochMillis);
                    log.debug("위시리스트 추가 업데이트 성공: productId={}", productId);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("위시리스트 추가 업데이트 스킵 - 락 획득 실패: productId={}", productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("위시리스트 추가 업데이트 중단 - 스레드 인터럽트: productId={}", productId);
        }
    }

    /**
     * 리뷰 작성 이벤트 처리 (메모리 락 적용)
     */
    public void incrementReview(Long productId, long occurredAtEpochMillis) {
        ReentrantLock lock = getProductLock(productId);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                try {
                    metricsTransactionService.incrementReviewWithTransaction(productId, occurredAtEpochMillis);
                    log.debug("리뷰 작성 업데이트 성공: productId={}", productId);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("리뷰 작성 업데이트 스킵 - 락 획득 실패: productId={}", productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("리뷰 작성 업데이트 중단 - 스레드 인터럽트: productId={}", productId);
        }
    }

    /**
     * 재고 소진 이벤트 처리 (캐시 무효화 중심)
     */
    public void handleStockDepleted(Long productId, Long brandId, long occurredAtEpochMillis) {
        // 재고 소진은 메트릭 업데이트보다는 캐시 무효화가 주 목적
        // 락 없이 바로 트랜잭션 서비스 호출
        metricsTransactionService.handleStockDepletedWithTransaction(productId, brandId, occurredAtEpochMillis);
        log.info("재고 소진 이벤트 처리 완료: productId={}, brandId={}", productId, brandId);
    }

    /**
     * 상품별 락 획득 (없으면 생성)
     */
    private ReentrantLock getProductLock(Long productId) {
        return productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
    }

    /**
     * 락 상태 모니터링 및 정리 (메모리 누수 방지)
     */
    public void cleanupUnusedLocks() {
        if (productLocks.size() > LOCK_CLEANUP_THRESHOLD) {
            log.info("락 정리 시작 - 현재 락 수: {}", productLocks.size());

            // 사용하지 않는 락 제거 (락이 걸려있지 않은 것들)
            productLocks.entrySet().removeIf(entry -> {
                ReentrantLock lock = entry.getValue();
                return !lock.isLocked() && !lock.hasQueuedThreads();
            });

            log.info("락 정리 완료 - 정리 후 락 수: {}", productLocks.size());
        }
    }

    /**
     * 처리된 이벤트 캐시 정리 (메모리 누수 방지)
     */
    public void cleanupProcessedEvents() {
        if (processedEvents.size() > PROCESSED_EVENTS_CLEANUP_THRESHOLD) {
            log.info("처리된 이벤트 캐시 정리 시작 - 현재 캐시 수: {}", processedEvents.size());
            
            // 오래된 이벤트 캐시 절반 정도 제거 (LRU 방식은 아니지만 메모리 절약)
            int targetSize = PROCESSED_EVENTS_CLEANUP_THRESHOLD / 2;
            int currentSize = processedEvents.size();
            int toRemove = currentSize - targetSize;
            
            processedEvents.entrySet().stream()
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .forEach(processedEvents::remove);
            
            log.info("처리된 이벤트 캐시 정리 완료 - 정리 후 캐시 수: {}", processedEvents.size());
        }
    }

    /**
     * 락 상태 정보 조회 (모니터링용)
     */
    public void logLockStatus() {
        int totalLocks = productLocks.size();
        long lockedCount = productLocks.values().stream()
                .mapToLong(lock -> lock.isLocked() ? 1 : 0)
                .sum();

        if (totalLocks > 0) {
            log.debug("메트릭 락 상태 - 총 락: {}, 사용 중: {}", totalLocks, lockedCount);
        }
    }
}
