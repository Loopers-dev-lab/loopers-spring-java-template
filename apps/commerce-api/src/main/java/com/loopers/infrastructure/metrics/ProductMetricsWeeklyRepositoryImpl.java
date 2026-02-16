package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.ProductMetricsWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 주간 상품 집계 Repository 구현 (읽기 전용)
 * commerce-collector에서 생성한 집계 데이터 조회
 */
@Component
@RequiredArgsConstructor
public class ProductMetricsWeeklyRepositoryImpl implements ProductMetricsWeeklyRepository {

    private final ProductMetricsWeeklyJpaRepository weeklyJpaRepository;

    @Override
    public List<ProductMetricsWeekly> findByYearAndWeekOrderByLikeCountDesc(int year, int week, int limit) {
        return weeklyJpaRepository.findByYearAndWeekOrderByTotalLikeCountDesc(
                year,
                week,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<ProductMetricsWeekly> findByYearAndWeekOrderByViewCountDesc(int year, int week, int limit) {
        return weeklyJpaRepository.findByYearAndWeekOrderByTotalViewCountDesc(
                year,
                week,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<ProductMetricsWeekly> findByYearAndWeekOrderByOrderCountDesc(int year, int week, int limit) {
        return weeklyJpaRepository.findByYearAndWeekOrderByTotalOrderCountDesc(
                year,
                week,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<ProductMetricsWeekly> findByYearAndWeekOrderByCompositeScoreDesc(int year, int week, int limit) {
        return weeklyJpaRepository.findByYearAndWeekOrderByCompositeScoreDesc(
                year,
                week,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public Optional<ProductMetricsWeekly> findByYearAndWeekAndProductId(int year, int week, Long productId) {
        return weeklyJpaRepository.findByYearAndWeekAndProductId(year, week, productId);
    }

    @Override
    public long countByYearAndWeek(int year, int week) {
        return weeklyJpaRepository.countByYearAndWeek(year, week);
    }
}
