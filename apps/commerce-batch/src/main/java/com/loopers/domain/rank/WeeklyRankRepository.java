package com.loopers.domain.rank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository interface for WeeklyProductRank entity.
 *
 * <p>Provides data access methods for weekly product ranking data.
 */
public interface WeeklyRankRepository extends JpaRepository<WeeklyProductRank, Long> {

    /**
     * Finds all rankings for a specific week ordered by rank position.
     *
     * @param yearWeek the year-week in ISO format (e.g., "2025-W01")
     * @return list of weekly rankings ordered by position
     */
    List<WeeklyProductRank> findByYearWeekOrderByRankPositionAsc(String yearWeek);

    /**
     * Deletes all rankings for a specific week.
     *
     * <p>Used before inserting new aggregated data to ensure data consistency.
     *
     * @param yearWeek the year-week to delete
     */
    @Modifying
    @Query("DELETE FROM WeeklyProductRank w WHERE w.yearWeek = :yearWeek")
    void deleteByYearWeek(@Param("yearWeek") String yearWeek);
}
