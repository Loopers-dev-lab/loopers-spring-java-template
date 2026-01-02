package com.loopers.domain.ranking;

import java.util.List;

public interface WeeklyRankingRepository {
    List<WeeklyRankingMv> getWeeklyTop100(String date, String lastWeek, int page, int size);
    long countWeekly(String startDate, String endDate);
}
