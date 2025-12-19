package com.loopers.application;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void upsertLikeCount(Long productId, long delta) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.builder().productId(productId).build());
        metrics.incrementLikeCount(delta);
        productMetricsRepository.save(metrics);
        log.debug("Upserted like count for productId {}: delta {}", productId, delta);
    }

    @Transactional
    public void upsertSalesCount(Long productId, long quantity) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.builder().productId(productId).build());
        metrics.incrementSalesCount(quantity);
        productMetricsRepository.save(metrics);
        log.debug("Upserted sales count for productId {}: quantity {}", productId, quantity);
    }

    @Transactional
    public void upsertViewCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.builder().productId(productId).build());
        metrics.incrementViewCount();
        productMetricsRepository.save(metrics);
        log.debug("Upserted view count for productId {}", productId);
    }
}
