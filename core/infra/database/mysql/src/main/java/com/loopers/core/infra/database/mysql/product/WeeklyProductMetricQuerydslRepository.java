package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.dto.WeeklyProductRankingProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface WeeklyProductMetricQuerydslRepository {

    Page<WeeklyProductRankingProjection> findWeeklyProductRanking(
            LocalDate date,
            Double payWeight,
            Double viewWeight,
            Double likeWeight,
            Pageable pageable
    );
}
