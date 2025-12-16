package com.loopers.domain.metrics;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.loopers.domain.event.EventEntity;
import com.loopers.domain.event.EventRepository;

import lombok.RequiredArgsConstructor;

import jakarta.transaction.Transactional;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Component
@RequiredArgsConstructor
public class MetricsService {
    private final EventRepository eventHandledRepository;
    private final ProductMetricsRepository productMetricsRepository;

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
    public void incrementView(Long productId) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        metrics.incrementView();
        productMetricsRepository.save(metrics);
    }

    @Transactional
    public void applyLikeDelta(final Long productId, final int delta) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        metrics.applyLikeDelta(delta);
        productMetricsRepository.save(metrics);
    }

    @Transactional
    public void addSales(final Long productId, final int quantity) {
        final ProductMetricsEntity metrics = getOrCreate(productId);
        metrics.addSales(quantity);
        productMetricsRepository.save(metrics);
    }

    private ProductMetricsEntity getOrCreate(final Long productId) {
        final Optional<ProductMetricsEntity> found = productMetricsRepository.findById(productId);
        return found.orElseGet(() -> ProductMetricsEntity.create(productId));
    }
}
