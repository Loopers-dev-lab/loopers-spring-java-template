package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void processLikeMetrics(Long productId, String likeType, LocalDate date) {
        ProductMetrics metrics = getOrCreateMetrics(productId, date);

        if ("LIKED".equals(likeType)) {
            metrics.incrementLikes();
        } else if ("UNLIKED".equals(likeType)) {
            metrics.decrementLikes();
        }

        productMetricsRepository.save(metrics);
    }

    @Transactional
    public void processStockMetrics(Long productId, int stock, String changedType, LocalDate date) {
        ProductMetrics metrics = getOrCreateMetrics(productId, date);

        if ("DECREASED".equals(changedType)) {
            metrics.incrementSales(stock);
        } else if ("RESTORED".equals(changedType)) {
            metrics.decrementSales(stock);
        }

        productMetricsRepository.save(metrics);
    }

    @Transactional
    public void processViewMetrics(Long productId, LocalDate date) {
        ProductMetrics metrics = getOrCreateMetrics(productId, date);
        metrics.incrementViews();
        productMetricsRepository.save(metrics);
    }

    private ProductMetrics getOrCreateMetrics(Long productId, LocalDate date) {
        return productMetricsRepository.findByProductIdAndDate(productId, date)
                .orElseGet(() -> ProductMetrics.create(productId, date));
    }
}
