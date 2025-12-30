package com.loopers.application;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    /**
     * 좋아요 카운트 업데이트 (멱등성 보장)
     * 
     * @param productId 상품 ID
     * @param delta 변경량 (+1 또는 -1)
     * @param eventOccurredAt 이벤트 발생 시각 (stale event 체크용)
     */
    @Transactional
    public void upsertLikeCount(Long productId, long delta, LocalDateTime eventOccurredAt) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.builder().productId(productId).build());

        // Stale event 체크: 이미 더 최신 이벤트가 처리되었는지 확인
        if (metrics.getLastEventOccurredAt() != null && 
            !eventOccurredAt.isAfter(metrics.getLastEventOccurredAt())) {
            log.info("Stale event detected, skipping - productId: {}, eventOccurredAt: {}, lastEventOccurredAt: {}", 
                    productId, eventOccurredAt, metrics.getLastEventOccurredAt());
            return;
        }

        metrics.incrementLikeCount(delta);
        metrics.updateLastEventOccurredAt(eventOccurredAt);
        productMetricsRepository.save(metrics);
        log.debug("Upserted like count for productId {}: delta {}", productId, delta);
    }

    /**
     * 좋아요 카운트 업데이트 (하위 호환성을 위한 오버로드)
     * @deprecated 이벤트의 occurredAt을 전달하는 버전을 사용하세요
     */
    @Deprecated
    @Transactional
    public void upsertLikeCount(Long productId, long delta) {
        upsertLikeCount(productId, delta, LocalDateTime.now());
    }

    @Transactional
    public void upsertSalesCount(Long productId, long quantity) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.builder().productId(productId).build());
        metrics.incrementSalesCount(quantity);
        productMetricsRepository.save(metrics);
        log.debug("Upserted sales count for productId {}: quantity {}", productId, quantity);
    }

    @Transactional
    public void upsertViewCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.builder().productId(productId).build());
        metrics.incrementViewCount();
        productMetricsRepository.save(metrics);
        log.debug("Upserted view count for productId {}", productId);
    }
}
