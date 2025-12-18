package com.loopers.application.service;

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
    public void incrementLikeCount(Long productId) {
        int updatedRows = productMetricsRepository.incrementLikeCount(productId);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId);
            metrics.incrementLikeCount();
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics and incremented like count for productId: {}", productId);
        } else {
            log.info("Incremented like count for productId: {}", productId);
        }
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        int updatedRows = productMetricsRepository.decrementLikeCount(productId);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성 (좋아요 취소는 0으로 유지)
            ProductMetrics metrics = new ProductMetrics(productId);
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics for productId: {}", productId);
        } else {
            log.info("Decremented like count for productId: {}", productId);
        }
    }

    @Transactional
    public void incrementSalesCount(Long productId, Long quantity) {
        int updatedRows = productMetricsRepository.incrementSalesCount(productId, quantity);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId);
            metrics.incrementSalesCount(quantity);
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics and incremented sales count for productId: {}, quantity: {}", productId, quantity);
        } else {
            log.info("Incremented sales count for productId: {}, quantity: {}", productId, quantity);
        }
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        int updatedRows = productMetricsRepository.incrementViewCount(productId);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId);
            metrics.incrementViewCount();
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics and incremented view count for productId: {}", productId);
        } else {
            log.info("Incremented view count for productId: {}", productId);
        }
    }
}