package com.loopers.infrastructure.ranking.monthly;

import com.loopers.domain.ranking.monthly.MonthlyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MonthlyRankingJpaRepository extends JpaRepository<MonthlyRanking, Long> {
    
    List<MonthlyRanking> findByMonthYearOrderByRankPosition(String monthYear);
    
    @Query("SELECT m FROM MonthlyRanking m WHERE m.monthYear = :monthYear " +
           "ORDER BY m.rankPosition LIMIT :limit OFFSET :offset")
    List<MonthlyRanking> findByMonthYearWithPagination(String monthYear, int offset, int limit);
    
    @Modifying
    @Transactional
    void deleteByMonthYear(String monthYear);
}