package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void incrementLikeCount(Long productId) {
        LocalDateTime bucketTime = getBucketTime();
        int updatedRows = productMetricsRepository.incrementLikeCount(productId, bucketTime);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId, bucketTime);
            metrics.incrementLikeCount();
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics and incremented like count for productId: {}, bucketTime: {}", productId, bucketTime);
        } else {
            log.info("Incremented like count for productId: {}, bucketTime: {}", productId, bucketTime);
        }
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        LocalDateTime bucketTime = getBucketTime();
        int updatedRows = productMetricsRepository.decrementLikeCount(productId, bucketTime);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성 (좋아요 취소는 0으로 유지)
            ProductMetrics metrics = new ProductMetrics(productId, bucketTime);
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics for productId: {}, bucketTime: {}", productId, bucketTime);
        } else {
            log.info("Decremented like count for productId: {}, bucketTime: {}", productId, bucketTime);
        }
    }

    @Transactional
    public void incrementSalesCount(Long productId, Long quantity) {
        LocalDateTime bucketTime = getBucketTime();
        int updatedRows = productMetricsRepository.incrementSalesCount(productId, bucketTime, quantity);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId, bucketTime);
            metrics.incrementSalesCount(quantity);
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics and incremented sales count for productId: {}, quantity: {}, bucketTime: {}", productId, quantity, bucketTime);
        } else {
            log.info("Incremented sales count for productId: {}, quantity: {}, bucketTime: {}", productId, quantity, bucketTime);
        }
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        LocalDateTime bucketTime = getBucketTime();
        int updatedRows = productMetricsRepository.incrementViewCount(productId, bucketTime);
        
        if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId, bucketTime);
            metrics.incrementViewCount();
            productMetricsRepository.save(metrics);
            log.info("Created new ProductMetrics and incremented view count for productId: {}, bucketTime: {}", productId, bucketTime);
        } else {
            log.info("Incremented view count for productId: {}, bucketTime: {}", productId, bucketTime);
        }
    }
    
    /**
     * 현재 시간을 10분 단위로 truncate한 bucketTime 생성
     * 예: 2024-12-19 14:35:22 -> 2024-12-19 14:30:00
     *     2024-12-19 14:42:15 -> 2024-12-19 14:40:00
     */
    private LocalDateTime getBucketTime() {
        LocalDateTime now = LocalDateTime.now();
        // 분을 10분 단위로 내림 (0, 10, 20, 30, 40, 50)
        int bucketMinutes = (now.getMinute() / 10) * 10;
        return now.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
    }
}
