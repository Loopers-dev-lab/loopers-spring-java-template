package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.MonthlyProductMetric;
import com.loopers.core.domain.product.repository.MonthlyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductRankings;
import com.loopers.core.infra.database.mysql.product.MonthlyProductMetricBulkRepository;
import com.loopers.core.infra.database.mysql.product.MonthlyProductMetricJpaRepository;
import com.loopers.core.infra.database.mysql.product.dto.MonthlyProductRankingProjection;
import com.loopers.core.infra.database.mysql.product.entity.MonthlyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MonthlyProductMetricRepositoryImpl implements MonthlyProductMetricRepository {

    private final MonthlyProductMetricBulkRepository bulkRepository;
    private final MonthlyProductMetricJpaRepository repository;

    @Override
    public void bulkUpsert(List<MonthlyProductMetric> monthlyProductMetrics) {
        bulkRepository.bulkUpsert(monthlyProductMetrics.stream()
                .map(MonthlyProductMetricEntity::from)
                .toList());
    }

    @Override
    public ProductRankings findRankingsBy(
            LocalDate date,
            Integer pageNo,
            Integer pageSize,
            Double payWeight,
            Double viewWeight,
            Double likeWeight
    ) {

        Page<MonthlyProductRankingProjection> page = repository.findMonthlyProductRanking(
                date,
                payWeight,
                viewWeight,
                likeWeight,
                PageRequest.of(pageNo, pageSize)
        );

        return new ProductRankings(
                page.stream()
                        .map(MonthlyProductRankingProjection::to)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
