package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface MvProductRankWeeklyRepository {
    List<MvProductRankWeekly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate, int page, int size);
}

