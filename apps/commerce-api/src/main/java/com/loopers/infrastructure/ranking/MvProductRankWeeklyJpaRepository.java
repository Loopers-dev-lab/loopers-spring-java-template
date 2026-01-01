package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MvProductRankWeeklyJpaRepository extends JpaRepository<MvProductRankWeekly, Long> {
    
    List<MvProductRankWeekly> findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            Pageable pageable
    );
}

