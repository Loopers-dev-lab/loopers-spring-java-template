package com.loopers.batch.domain.ranking;

import java.time.YearMonth;
import java.util.List;

public interface MonthlyRankingRepository {

    void deleteByMonthPeriod(YearMonth monthPeriod);

    void saveAll(List<MonthlyRanking> rankings);
}
