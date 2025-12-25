package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductMetricsService {
    private final ProductMetricsRepository productMetricsRepository;

    /**
     * 좋아요 수 증가
     */
    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.incrementLikeCount();
    }

    /**
     * 좋아요 수 감소
     */
    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.decrementLikeCount();
    }

    /**
     * 주문 수 증가
     */
    @Transactional
    public void incrementOrderCount(Long productId, int quantity) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.incrementOrderCount(quantity);
    }

    /**
     * 상품 조회 수 증가
     * */
    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = getOrCreateMetrics(productId);
        metrics.incrementViewCount();
    }

    /**
     * Metrics 조회 또는 생성 (비관적 락 사용)
     * 동시성 제어: 같은 productId에 대한 동시 접근 시 순차 처리
     */
    private ProductMetrics getOrCreateMetrics(Long productId) {
        // 비관적 락을 걸고 조회 (다른 트랜잭션은 대기)
        return productMetricsRepository.findByProductIdWithLock(productId)
                .orElseGet(() -> {
                    try {
                        // 락을 획득했고 없으면 생성
                        ProductMetrics newMetrics = ProductMetrics.create(productId);
                        return productMetricsRepository.save(newMetrics);
                    } catch (Exception e) {
                        // Unique constraint violation 시 재조회
                        // (드물지만 락 획득 전 다른 트랜잭션이 생성한 경우)
                        return productMetricsRepository.findByProductIdWithLock(productId)
                                .orElseThrow(() -> new RuntimeException("ProductMetrics 조회 실패: " + productId, e));
                    }
                });
    }
}
