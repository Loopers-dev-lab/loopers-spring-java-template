package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MvProductRankMonthlyRepository {
    Optional<MvProductRankMonthly> findByProductIdAndPeriod(Long productId, LocalDate periodStartDate, LocalDate periodEndDate);

    List<MvProductRankMonthly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate);

    void save(MvProductRankMonthly rank);

    void delete(MvProductRankMonthly rank);
}

