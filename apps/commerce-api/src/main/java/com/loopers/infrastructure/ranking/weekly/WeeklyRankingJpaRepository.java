package com.loopers.infrastructure.ranking.weekly;

import com.loopers.domain.ranking.weekly.WeeklyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyRankingJpaRepository extends JpaRepository<WeeklyRanking, Long> {
    
    List<WeeklyRanking> findByWeekStartDateOrderByRankPosition(LocalDate weekStartDate);
    
    @Query("SELECT w FROM WeeklyRanking w WHERE w.weekStartDate = :weekStartDate " +
           "ORDER BY w.rankPosition LIMIT :limit OFFSET :offset")
    List<WeeklyRanking> findByWeekStartDateWithPagination(LocalDate weekStartDate, int offset, int limit);
    
    @Modifying
    @Transactional
    void deleteByWeekStartDate(LocalDate weekStartDate);
}