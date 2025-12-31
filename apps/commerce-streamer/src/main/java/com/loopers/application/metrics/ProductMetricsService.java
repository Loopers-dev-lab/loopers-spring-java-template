package com.loopers.application.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.incrementLikeCount();
        productMetricsRepository.save(metrics);
        log.info("좋아요 수 증가 - productId: {}, likeCount: {}", productId, metrics.getLikeCount());
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.decrementLikeCount();
        productMetricsRepository.save(metrics);
        log.info("좋아요 수 감소 - productId: {}, likeCount: {}", productId, metrics.getLikeCount());
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.incrementViewCount();
        productMetricsRepository.save(metrics);
        log.info("조회 수 증가 - productId: {}, viewCount: {}", productId, metrics.getViewCount());
    }

    @Transactional
    public void incrementOrderMetrics(Long productId, int quantity, BigDecimal amount) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.incrementOrderCount(quantity, amount);
        productMetricsRepository.save(metrics);
        log.info("주문 지표 증가 - productId: {}, orderCount: {}, salesAmount: {}",
                productId, metrics.getOrderCount(), metrics.getSalesAmount());
    }

    private ProductMetrics getOrCreate(Long productId) {
        return productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.create(productId));
    }
}



