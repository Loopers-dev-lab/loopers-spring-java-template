package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsMonthlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 월간 상품 집계 Repository 구현 (읽기 전용)
 * commerce-collector에서 생성한 집계 데이터 조회
 */
@Component
@RequiredArgsConstructor
public class ProductMetricsMonthlyRepositoryImpl implements ProductMetricsMonthlyRepository {

    private final ProductMetricsMonthlyJpaRepository monthlyJpaRepository;

    @Override
    public List<ProductMetricsMonthly> findByYearAndMonthOrderByLikeCountDesc(int year, int month, int limit) {
        return monthlyJpaRepository.findByYearAndMonthOrderByTotalLikeCountDesc(
                year,
                month,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<ProductMetricsMonthly> findByYearAndMonthOrderByViewCountDesc(int year, int month, int limit) {
        return monthlyJpaRepository.findByYearAndMonthOrderByTotalViewCountDesc(
                year,
                month,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<ProductMetricsMonthly> findByYearAndMonthOrderByOrderCountDesc(int year, int month, int limit) {
        return monthlyJpaRepository.findByYearAndMonthOrderByTotalOrderCountDesc(
                year,
                month,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<ProductMetricsMonthly> findByYearAndMonthOrderByCompositeScoreDesc(int year, int month, int limit) {
        return monthlyJpaRepository.findByYearAndMonthOrderByCompositeScoreDesc(
                year,
                month,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public Optional<ProductMetricsMonthly> findByYearAndMonthAndProductId(int year, int month, Long productId) {
        return monthlyJpaRepository.findByYearAndMonthAndProductId(year, month, productId);
    }

    @Override
    public long countByYearAndMonth(int year, int month) {
        return monthlyJpaRepository.countByYearAndMonth(year, month);
    }
}
