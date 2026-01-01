package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MvProductRankMonthlyJpaRepository extends JpaRepository<MvProductRankMonthly, Long> {
    
    Optional<MvProductRankMonthly> findByProductIdAndPeriodStartDateAndPeriodEndDate(
            Long productId,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    );
    
    List<MvProductRankMonthly> findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(
            LocalDate periodStartDate,
            LocalDate periodEndDate
    );
}


