package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MvProductRankWeeklyJpaRepository extends JpaRepository<MvProductRankWeekly, Long> {
    
    Optional<MvProductRankWeekly> findByProductIdAndPeriodStartDateAndPeriodEndDate(
            Long productId,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    );
    
    List<MvProductRankWeekly> findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(
            LocalDate periodStartDate,
            LocalDate periodEndDate
    );
}


