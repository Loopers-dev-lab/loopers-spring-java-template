package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface MonthlyRankingJpaRepository extends JpaRepository<MonthlyRanking, Long> {

    @Query(value = """
            SELECT * FROM mv_product_rank_monthly 
            WHERE month_period = :monthPeriod 
            ORDER BY monthly_rank ASC 
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<MonthlyRanking> findByPeriodWithPagination(
            @Param("monthPeriod") String monthPeriod,
            @Param("limit") int limit,
            @Param("offset") int offset);

    Optional<MonthlyRanking> findByProductIdAndMonthPeriod(Long productId, YearMonth monthPeriod);

    long countByMonthPeriod(YearMonth monthPeriod);
}
