package com.loopers.batch.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyRankingRepository {

    void deleteByWeekStartAndWeekEnd(LocalDate weekStart, LocalDate weekEnd);

    void saveAll(List<WeeklyRanking> rankings);
}
