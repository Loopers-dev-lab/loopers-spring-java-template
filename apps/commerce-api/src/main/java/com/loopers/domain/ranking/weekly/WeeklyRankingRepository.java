package com.loopers.domain.ranking.weekly;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyRankingRepository {
    
    List<WeeklyRanking> findByWeekStartDateOrderByRankPosition(LocalDate weekStartDate);
    
    List<WeeklyRanking> findByWeekStartDateWithPagination(LocalDate weekStartDate, int offset, int limit);
    
    void deleteByWeekStartDate(LocalDate weekStartDate);
    
    void saveAll(List<WeeklyRanking> rankings);
}