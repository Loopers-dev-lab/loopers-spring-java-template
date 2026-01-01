package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.WeeklyProductMetric;
import com.loopers.core.domain.product.repository.WeeklyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductRankings;
import com.loopers.core.infra.database.mysql.product.WeeklyProductMetricBulkRepository;
import com.loopers.core.infra.database.mysql.product.WeeklyProductMetricJpaRepository;
import com.loopers.core.infra.database.mysql.product.dto.WeeklyProductRankingProjection;
import com.loopers.core.infra.database.mysql.product.entity.WeeklyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WeeklyProductMetricRepositoryImpl implements WeeklyProductMetricRepository {

    private final WeeklyProductMetricJpaRepository repository;
    private final WeeklyProductMetricBulkRepository bulkRepository;

    @Override
    public void bulkUpsert(List<WeeklyProductMetric> weeklyProductMetrics) {
        List<WeeklyProductMetricEntity> entities = weeklyProductMetrics.stream()
                .map(WeeklyProductMetricEntity::from)
                .toList();
        bulkRepository.bulkUpsert(entities);
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

        Page<WeeklyProductRankingProjection> page = repository.findWeeklyProductRanking(
                date,
                payWeight,
                viewWeight,
                likeWeight,
                PageRequest.of(pageNo, pageSize)
        );

        return new ProductRankings(
                page.stream()
                        .map(WeeklyProductRankingProjection::to)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
