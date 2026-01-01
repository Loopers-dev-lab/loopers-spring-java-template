package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void incrementLikeCount(Long productId) {
        LocalDate today = LocalDate.now();
        int updatedRows = productMetricsRepository.incrementLikeCount(productId, today);

        if (updatedRows == 0) {
            try {
                ProductMetrics newMetrics = ProductMetrics.create(productId, today);
                newMetrics.incrementLikeCount();
                productMetricsRepository.saveProductMetrics(newMetrics);
            } catch (DataIntegrityViolationException e) {
                productMetricsRepository.incrementLikeCount(productId, today);
            }
        }
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        LocalDate today = LocalDate.now();
        ProductMetrics productMetrics = productMetricsRepository.findByProductIdAndMetricsDate(productId, today)
                .orElseGet(() -> {
                    log.warn("좋아요 취소 시 메트릭이 존재하지 않음: productId={}, metricsDate={}", productId, today);
                    return ProductMetrics.create(productId, today);
                });
        productMetrics.decrementLikeCount();
        productMetricsRepository.saveProductMetrics(productMetrics);
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        LocalDate today = LocalDate.now();
        ProductMetrics metrics = productMetricsRepository.findByProductIdAndMetricsDate(productId, today)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId, today);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementViewCount();
        productMetricsRepository.saveProductMetrics(metrics);
    }

    @Transactional
    public void incrementSalesCount(Long productId, Integer quantity) {
        LocalDate today = LocalDate.now();
        ProductMetrics metrics = productMetricsRepository.findByProductIdAndMetricsDate(productId, today)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId, today);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementSalesCount(quantity);
        productMetricsRepository.saveProductMetrics(metrics);
    }
}

