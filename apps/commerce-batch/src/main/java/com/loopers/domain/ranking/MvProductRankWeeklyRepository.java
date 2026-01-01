package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MvProductRankWeeklyRepository {
    Optional<MvProductRankWeekly> findByProductIdAndPeriod(Long productId, LocalDate periodStartDate, LocalDate periodEndDate);

    List<MvProductRankWeekly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate);

    void save(MvProductRankWeekly rank);

    void delete(MvProductRankWeekly rank);
}

