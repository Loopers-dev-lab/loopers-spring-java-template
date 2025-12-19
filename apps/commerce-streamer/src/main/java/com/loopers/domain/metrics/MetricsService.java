package com.loopers.domain.metrics;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.loopers.domain.event.EventEntity;
import com.loopers.domain.event.EventRepository;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.lock.RedisDistributedLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.transaction.Transactional;

/**
 * Redis 분산락을 이용한 동시성 안전한 메트릭 서비스
 * <p>
 * 동일한 상품에 대한 동시 업데이트를 분산락으로 제어하여
 * 메트릭 누락 없이 원자적으로 처리합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsService {
    private final EventRepository eventHandledRepository;
    private final ProductMetricsRepository productMetricsRepository;
    private final ProductCacheService productCacheService;
    private final RedisDistributedLock distributedLock;
    
    // 분산락 설정
    private static final Duration LOCK_EXPIRE_TIME = Duration.ofSeconds(10); // 락 만료 시간
    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(5);     // 최대 대기 시간

    @Transactional
    public boolean tryMarkHandled(String eventId) {
        try {
            eventHandledRepository.save(EventEntity.create(eventId));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false; // 이미 처리됨
        }
    }

    /**
     * 조회수 증가 (분산락 적용)
     */
    public void incrementView(Long productId, long occurredAtEpochMillis) {
        String lockKey = "metrics:view:" + productId;
        String lockValue = generateLockValue();
        
        boolean success = distributedLock.executeWithLock(
            lockKey, 
            lockValue, 
            LOCK_EXPIRE_TIME, 
            MAX_WAIT_TIME,
            () -> incrementViewWithLock(productId, occurredAtEpochMillis)
        );
        
        if (!success) {
            log.error("조회수 업데이트 실패 - 분산락 획득 실패: productId={}", productId);
            throw new RuntimeException("조회수 업데이트 실패: 분산락 획득 실패");
        }
    }
    
    @Transactional
    protected void incrementViewWithLock(Long productId, long occurredAtEpochMillis) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        final ZonedDateTime eventTime = toZonedDateTime(occurredAtEpochMillis);
        
        if (isEventTooOld(metrics, eventTime)) {
            log.debug("Ignoring old PRODUCT_VIEW event for productId: {}, eventTime: {}, lastEventAt: {}", 
                     productId, eventTime, metrics.getLastEventAt());
            return;
        }
        
        metrics.incrementView(eventTime);
        productMetricsRepository.save(metrics);
        
        log.debug("조회수 업데이트 완료 - productId: {}, newViewCount: {}", productId, metrics.getViewCount());
        
        // 캐시 무효화 (조회수 임계값 체크)
        productCacheService.onViewCountChanged(productId, metrics.getViewCount());
    }

    /**
     * 좋아요 수 변경 (분산락 적용)
     */
    public void applyLikeDelta(final Long productId, final int delta, long occurredAtEpochMillis) {
        String lockKey = "metrics:like:" + productId;
        String lockValue = generateLockValue();
        
        boolean success = distributedLock.executeWithLock(
            lockKey, 
            lockValue, 
            LOCK_EXPIRE_TIME, 
            MAX_WAIT_TIME,
            () -> applyLikeDeltaWithLock(productId, delta, occurredAtEpochMillis)
        );
        
        if (!success) {
            log.error("좋아요 수 업데이트 실패 - 분산락 획득 실패: productId={}, delta={}", productId, delta);
            throw new RuntimeException("좋아요 수 업데이트 실패: 분산락 획득 실패");
        }
    }
    
    @Transactional
    protected void applyLikeDeltaWithLock(final Long productId, final int delta, long occurredAtEpochMillis) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        final ZonedDateTime eventTime = toZonedDateTime(occurredAtEpochMillis);
        
        if (isEventTooOld(metrics, eventTime)) {
            log.debug("Ignoring old LIKE_ACTION event for productId: {}, eventTime: {}, lastEventAt: {}", 
                     productId, eventTime, metrics.getLastEventAt());
            return;
        }
        
        long oldLikeCount = metrics.getLikeCount();
        metrics.applyLikeDelta(delta, eventTime);
        productMetricsRepository.save(metrics);
        
        log.debug("좋아요 수 업데이트 완료 - productId: {}, delta: {}, oldCount: {}, newCount: {}", 
                 productId, delta, oldLikeCount, metrics.getLikeCount());
        
        // 캐시 무효화 (좋아요 수 변경)
        productCacheService.onLikeCountChanged(productId);
    }

    /**
     * 판매량 증가 (분산락 적용)
     */
    public void addSales(final Long productId, final int quantity, long occurredAtEpochMillis) {
        String lockKey = "metrics:sales:" + productId;
        String lockValue = generateLockValue();
        
        boolean success = distributedLock.executeWithLock(
            lockKey, 
            lockValue, 
            LOCK_EXPIRE_TIME, 
            MAX_WAIT_TIME,
            () -> addSalesWithLock(productId, quantity, occurredAtEpochMillis)
        );
        
        if (!success) {
            log.error("판매량 업데이트 실패 - 분산락 획득 실패: productId={}, quantity={}", productId, quantity);
            throw new RuntimeException("판매량 업데이트 실패: 분산락 획득 실패");
        }
    }
    
    @Transactional
    protected void addSalesWithLock(final Long productId, final int quantity, long occurredAtEpochMillis) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        final ZonedDateTime eventTime = toZonedDateTime(occurredAtEpochMillis);
        
        if (isEventTooOld(metrics, eventTime)) {
            log.debug("Ignoring old PAYMENT_SUCCESS event for productId: {}, eventTime: {}, lastEventAt: {}", 
                     productId, eventTime, metrics.getLastEventAt());
            return;
        }
        
        long oldSalesCount = metrics.getSalesCount();
        metrics.addSales(quantity, eventTime);
        productMetricsRepository.save(metrics);
        
        log.debug("판매량 업데이트 완료 - productId: {}, quantity: {}, oldCount: {}, newCount: {}", 
                 productId, quantity, oldSalesCount, metrics.getSalesCount());
        
        // 캐시 무효화 (판매량 변경 - 인기 상품 순위 영향)
        productCacheService.onSalesCountChanged(productId);
    }

    private ProductMetricsEntity getOrCreate(final Long productId) {
        final Optional<ProductMetricsEntity> found = productMetricsRepository.findById(productId);
        return found.orElseGet(() -> ProductMetricsEntity.create(productId));
    }
    
    private ZonedDateTime toZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
    
    private boolean isEventTooOld(ProductMetricsEntity metrics, ZonedDateTime eventTime) {
        // 첫 번째 이벤트인 경우 항상 처리
        if (metrics.getLastEventAt() == null) {
            return false;
        }
        
        // 이벤트 시간이 마지막 처리 시간보다 이전인 경우 무시
        return eventTime.isBefore(metrics.getLastEventAt());
    }
    
    /**
     * 분산락용 고유 값 생성
     */
    private String generateLockValue() {
        return Thread.currentThread().getName() + ":" + UUID.randomUUID().toString();
    }
}
