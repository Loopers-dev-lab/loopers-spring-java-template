package com.loopers.domain.metrics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.loopers.domain.event.EventEntity;
import com.loopers.domain.event.EventRepository;
import com.loopers.infrastructure.cache.ProductCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.transaction.Transactional;

/**
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

    @Transactional
    public boolean tryMarkHandled(String eventId) {
        try {
            eventHandledRepository.save(EventEntity.create(eventId));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false; // 이미 처리됨
        }
    }

    @Transactional
    public void incrementView(Long productId, long occurredAtEpochMillis) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        final ZonedDateTime eventTime = toZonedDateTime(occurredAtEpochMillis);
        
        if (isEventTooOld(metrics, eventTime)) {
            log.debug("Ignoring old PRODUCT_VIEW event for productId: {}, eventTime: {}, lastEventAt: {}", 
                     productId, eventTime, metrics.getLastEventAt());
            return;
        }
        
        metrics.incrementView(eventTime);
        productMetricsRepository.save(metrics);
        
        // 캐시 무효화 (조회수 임계값 체크)
        productCacheService.onViewCountChanged(productId, metrics.getViewCount());
    }

    @Transactional
    public void applyLikeDelta(final Long productId, final int delta, long occurredAtEpochMillis) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        final ZonedDateTime eventTime = toZonedDateTime(occurredAtEpochMillis);
        
        if (isEventTooOld(metrics, eventTime)) {
            log.debug("Ignoring old LIKE_ACTION event for productId: {}, eventTime: {}, lastEventAt: {}", 
                     productId, eventTime, metrics.getLastEventAt());
            return;
        }
        
        metrics.applyLikeDelta(delta, eventTime);
        productMetricsRepository.save(metrics);
        
        // 캐시 무효화 (좋아요 수 변경)
        productCacheService.onLikeCountChanged(productId);
    }

    @Transactional
    public void addSales(final Long productId, final int quantity, long occurredAtEpochMillis) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        final ZonedDateTime eventTime = toZonedDateTime(occurredAtEpochMillis);
        
        if (isEventTooOld(metrics, eventTime)) {
            log.debug("Ignoring old PAYMENT_SUCCESS event for productId: {}, eventTime: {}, lastEventAt: {}", 
                     productId, eventTime, metrics.getLastEventAt());
            return;
        }
        
        metrics.addSales(quantity, eventTime);
        productMetricsRepository.save(metrics);
        
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
}
