package com.loopers.infrastructure.ranking;

import com.loopers.batch.domain.ranking.WeeklyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface WeeklyRankingJpaRepository extends JpaRepository<WeeklyRanking, Long> {

    @Modifying
    @Query("DELETE FROM WeeklyRanking w WHERE w.weekStart = :weekStart AND w.weekEnd = :weekEnd")
    void deleteByWeekStartAndWeekEnd(@Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);
}
