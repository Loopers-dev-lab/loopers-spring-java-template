package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductMetricsMonthlyJpaRepository extends JpaRepository<ProductMetricsMonthly, Long> {

    @Modifying
    @Query("""
        DELETE FROM ProductMetricsMonthly m
        WHERE (m.year < :year)
           OR (m.year = :year AND m.month < :month)
    """)
    int deleteByYearAndMonthBefore(
            @Param("year") Integer year,
            @Param("month") Integer month
    );

}
