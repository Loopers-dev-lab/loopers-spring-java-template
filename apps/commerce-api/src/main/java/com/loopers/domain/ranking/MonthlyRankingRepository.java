package com.loopers.domain.ranking;

import java.util.List;

public interface MonthlyRankingRepository {
    List<MonthlyRankingMv> getMonthlyTop100(String endDate, String startDate, int safePage, int safeSize);
    long countMonthly(String startDate, String endDate);

}
