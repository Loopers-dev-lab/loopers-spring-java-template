package com.loopers.application.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProductMetrics Service
 * - 상품별 집계 데이터 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    /**
     * 좋아요 수 증가
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.incrementLikeCount();
        productMetricsRepository.save(metrics);

        log.info("좋아요 수 증가 - productId: {}, likeCount: {}", productId, metrics.getLikeCount());
    }

    /**
     * 좋아요 수 감소
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.decrementLikeCount();
        productMetricsRepository.save(metrics);

        log.info("좋아요 수 감소 - productId: {}, likeCount: {}", productId, metrics.getLikeCount());
    }

    /**
     * 조회 수 증가
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.incrementViewCount();
        productMetricsRepository.save(metrics);

        log.info("조회 수 증가 - productId: {}, viewCount: {}", productId, metrics.getViewCount());
    }

    /**
     * 주문 수 및 판매 금액 증가
     *
     * @param productId 상품 ID
     * @param quantity 수량
     * @param amount 금액
     */
    @Transactional
    public void incrementOrderCount(Long productId, int quantity, BigDecimal amount) {
        ProductMetrics metrics = getOrCreate(productId);
        metrics.incrementOrderCount(quantity, amount);
        productMetricsRepository.save(metrics);

        log.info("주문 수 증가 - productId: {}, orderCount: {}, salesAmount: {}",
            productId, metrics.getOrderCount(), metrics.getSalesAmount());
    }

    /**
     * ProductMetrics 조회 또는 생성
     */
    private ProductMetrics getOrCreate(Long productId) {
        return productMetricsRepository.findByProductId(productId)
            .orElseGet(() -> ProductMetrics.builder()
                .productId(productId)
                .build());
    }
}
