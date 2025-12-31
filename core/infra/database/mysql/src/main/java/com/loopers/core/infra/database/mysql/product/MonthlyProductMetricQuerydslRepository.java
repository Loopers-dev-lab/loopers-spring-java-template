package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.dto.MonthlyProductRankingProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface MonthlyProductMetricQuerydslRepository {

    Page<MonthlyProductRankingProjection> findMonthlyProductRanking(
            LocalDate date,
            Double payWeight,
            Double viewWeight,
            Double likeWeight,
            Pageable pageable
    );
}
