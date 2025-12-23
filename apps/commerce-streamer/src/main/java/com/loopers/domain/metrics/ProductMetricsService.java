package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void incrementLikeCount(Long productId) {
        int updatedRows = productMetricsRepository.incrementLikeCount(productId);

        if (updatedRows == 0) {
            try {
                ProductMetrics newMetrics = ProductMetrics.create(productId);
                newMetrics.incrementLikeCount();
                productMetricsRepository.saveProductMetrics(newMetrics);
            } catch (DataIntegrityViolationException e) {
                productMetricsRepository.incrementLikeCount(productId);
            }
        }
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics productMetrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> {
                    log.warn("좋아요 취소 시 메트릭이 존재하지 않음: productId={}", productId);
                    return ProductMetrics.create(productId);
                });
        productMetrics.decrementLikeCount();
        productMetricsRepository.saveProductMetrics(productMetrics);
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementViewCount();
        productMetricsRepository.saveProductMetrics(metrics);
    }

    @Transactional
    public void incrementSalesCount(Long productId, Integer quantity) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementSalesCount(quantity);
        productMetricsRepository.saveProductMetrics(metrics);
    }
}

