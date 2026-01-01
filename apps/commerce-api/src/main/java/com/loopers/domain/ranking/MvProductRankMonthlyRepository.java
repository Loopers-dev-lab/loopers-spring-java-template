package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface MvProductRankMonthlyRepository {
    List<MvProductRankMonthly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate, int page, int size);
}

