package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void applyLikeDelta(Long productId, long delta, long version) {
        if (productId == null) {
            return;
        }
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.initialize(productId));
        if (metrics.applyLikeDelta(delta, version)) {
            productMetricsRepository.save(metrics);
        }
    }

    @Transactional
    public void increaseSales(Long productId, long quantity, long version) {
        if (productId == null || quantity <= 0) {
            return;
        }
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.initialize(productId));
        if (metrics.increaseSales(quantity, version)) {
            productMetricsRepository.save(metrics);
        }
    }
}
