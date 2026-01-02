package com.loopers.domain.metrics.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsDailyService {
    
    private final ProductMetricsDailyRepository repository;
    
    /**
     * 일별 메트릭 조회 또는 생성
     * 
     * @param productId 상품 ID
     * @param brandId 브랜드 ID
     * @param date 날짜
     * @return 일별 메트릭 (없으면 생성)
     */
    @Transactional
    public ProductMetricsDaily getOrCreate(Long productId, Long brandId, LocalDate date) {
        return repository.findByProductIdAndDate(productId, date)
            .orElseGet(() -> {
                ProductMetricsDaily daily = ProductMetricsDaily.create(productId, date);
                return repository.save(daily);
            });
    }
    
    /**
     * 좋아요 수 증가 (락 사용)
     */
    @Transactional
    public void incrementLikeCount(Long productId, LocalDate date) {
        ProductMetricsDaily daily = repository.findByProductIdAndDateForUpdate(productId, date)
            .orElseGet(() -> {
                ProductMetricsDaily newDaily = ProductMetricsDaily.create(productId, date);
                return repository.save(newDaily);
            });
        daily.incrementLikeCount();
        repository.save(daily);
        log.debug("Incremented daily like count: productId={}, date={}", productId, date);
    }
    
    /**
     * 좋아요 수 감소 (락 사용)
     */
    @Transactional
    public void decrementLikeCount(Long productId, LocalDate date) {
        ProductMetricsDaily daily = repository.findByProductIdAndDateForUpdate(productId, date)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, 
                "해당 상품의 일별 메트릭 정보를 찾을 수 없습니다."));
        daily.decrementLikeCount();
        repository.save(daily);
        log.debug("Decremented daily like count: productId={}, date={}", productId, date);
    }
    
    /**
     * 조회 수 증가 (락 사용)
     */
    @Transactional
    public void incrementViewCount(Long productId, LocalDate date) {
        ProductMetricsDaily daily = repository.findByProductIdAndDateForUpdate(productId, date)
            .orElseGet(() -> {
                ProductMetricsDaily newDaily = ProductMetricsDaily.create(productId, date);
                return repository.save(newDaily);
            });
        daily.incrementViewCount();
        repository.save(daily);
        log.debug("Incremented daily view count: productId={}, date={}", productId, date);
    }
    
    /**
     * 판매 수 증가 (락 사용)
     */
    @Transactional
    public void incrementSoldCount(Long productId, LocalDate date, Long quantity) {
        ProductMetricsDaily daily = repository.findByProductIdAndDateForUpdate(productId, date)
            .orElseGet(() -> {
                ProductMetricsDaily newDaily = ProductMetricsDaily.create(productId, date);
                return repository.save(newDaily);
            });
        daily.incrementSoldCount(quantity);
        repository.save(daily);
        log.debug("Incremented daily sold count: productId={}, date={}, quantity={}", 
            productId, date, quantity);
    }
}


