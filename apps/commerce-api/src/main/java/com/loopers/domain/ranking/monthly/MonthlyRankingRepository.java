package com.loopers.domain.ranking.monthly;

import java.util.List;

public interface MonthlyRankingRepository {
    
    List<MonthlyRanking> findByMonthYearOrderByRankPosition(String monthYear);
    
    List<MonthlyRanking> findByMonthYearWithPagination(String monthYear, int offset, int limit);
    
    void deleteByMonthYear(String monthYear);
    
    void saveAll(List<MonthlyRanking> rankings);
}