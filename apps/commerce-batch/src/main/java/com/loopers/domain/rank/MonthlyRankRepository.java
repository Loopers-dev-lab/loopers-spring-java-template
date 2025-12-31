package com.loopers.domain.rank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository interface for MonthlyProductRank entity.
 *
 * <p>Provides data access methods for monthly product ranking data.
 */
public interface MonthlyRankRepository extends JpaRepository<MonthlyProductRank, Long> {

    /**
     * Finds all rankings for a specific month ordered by rank position.
     *
     * @param yearMonth the year-month (e.g., "2025-01")
     * @return list of monthly rankings ordered by position
     */
    List<MonthlyProductRank> findByYearMonthOrderByRankPositionAsc(String yearMonth);

    /**
     * Deletes all rankings for a specific month.
     *
     * <p>Used before inserting new aggregated data to ensure data consistency.
     *
     * @param yearMonth the year-month to delete
     */
    @Modifying
    @Query("DELETE FROM MonthlyProductRank m WHERE m.yearMonth = :yearMonth")
    void deleteByYearMonth(@Param("yearMonth") String yearMonth);
}
