package com.loopers.application.metrics;

import com.loopers.domain.metrics.ProductMetricsDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductMetricsDailyFacade {

    private final ProductMetricsDailyRepository productMetricsDailyRepository;

    /**
     * 일자별 좋아요 증감 배치 업데이트
     * 상위 트랜잭션에 참여하여 원자성 보장
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLikeDeltaBatch(Map<Long, Integer> likeDeltas, LocalDate metricDate) {
        productMetricsDailyRepository.upsertLikeDeltas(likeDeltas, metricDate);
    }

    /**
     * 일자별 조회수 증감 배치 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateViewDeltaBatch(Map<Long, Integer> viewDeltas, LocalDate metricDate) {
        productMetricsDailyRepository.upsertViewDeltas(viewDeltas, metricDate);
    }

    /**
     * 일자별 주문 증감 배치 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateOrderMetricsBatch(Map<Long, com.loopers.application.order.OrderMetrics> orderMetrics, LocalDate metricDate) {
        productMetricsDailyRepository.upsertOrderDeltas(orderMetrics, metricDate);
    }
}
