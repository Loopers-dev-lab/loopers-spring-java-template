package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MvProductRankMonthlyJpaRepository extends JpaRepository<MvProductRankMonthly, Long> {

    List<MvProductRankMonthly> findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            Pageable pageable
    );

}

