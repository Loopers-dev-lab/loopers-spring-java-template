package com.loopers.application.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsFacade {
    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.incrementLikeCount();
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.decrementLikeCount();
    }

    @Transactional
    public void incrementOrderCount(Long productId, int quantity) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.incrementOrderCount(quantity);
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.incrementViewCount();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLikeCountBatch(Map<Long, Integer> likeDeltas) {
        productMetricsRepository.upsertLikeDeltas(likeDeltas);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateViewCountBatch(Map<Long, Integer> viewDeltas) {
        productMetricsRepository.upsertViewDeltas(viewDeltas);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateOrderMetricsBatch(Map<Long, com.loopers.application.order.OrderMetrics> orderMetrics) {
        productMetricsRepository.upsertOrderDeltas(orderMetrics);
    }

    private ProductMetrics getOrCreateMetrics(Long productId) {
        return productMetricsRepository.findByProductIdWithLock(productId)
                .orElseGet(() -> {
                    try {
                        ProductMetrics newMetrics = ProductMetrics.create(productId);
                        return productMetricsRepository.save(newMetrics);
                    } catch (Exception e) {
                        return productMetricsRepository.findByProductIdWithLock(productId)
                                .orElseThrow(() -> new RuntimeException("ProductMetrics 조회 실패: " + productId, e));
                    }
                });
    }
}