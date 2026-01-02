package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.WeeklyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyRankingJpaRepository extends JpaRepository<WeeklyRanking, Long> {

    @Query(value = """
            SELECT * FROM mv_product_rank_weekly 
            WHERE week_start = :weekStart AND week_end = :weekEnd 
            ORDER BY weekly_rank ASC 
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<WeeklyRanking> findByDateWithPagination(
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            @Param("limit") int limit,
            @Param("offset") int offset);

    Optional<WeeklyRanking> findByProductIdAndWeekStartAndWeekEnd(Long productId, LocalDate weekStart, LocalDate weekEnd);

    long countByWeekStartAndWeekEnd(LocalDate weekStart, LocalDate weekEnd);
}
