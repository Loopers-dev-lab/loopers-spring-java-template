package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductMetricsWeeklyJpaRepository extends JpaRepository<ProductMetricsWeekly, Long> {

    @Modifying
    @Query("""
        DELETE FROM ProductMetricsWeekly m
        WHERE (m.year < :year)
           OR (m.year = :year AND m.week < :week)
    """)
    int deleteByYearAndWeekBefore(
            @Param("year") Integer year,
            @Param("week") Integer week
    );

}
