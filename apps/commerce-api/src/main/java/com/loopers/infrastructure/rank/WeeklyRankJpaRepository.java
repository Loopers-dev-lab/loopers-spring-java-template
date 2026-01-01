package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.WeeklyProductRank;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA Repository for WeeklyProductRank entity.
 */
public interface WeeklyRankJpaRepository extends JpaRepository<WeeklyProductRank, Long> {

    /**
     * Finds weekly rankings for a specific week with pagination.
     *
     * @param yearWeek the year-week in ISO format (e.g., "2025-W01")
     * @param pageable pagination parameters
     * @return list of weekly rankings
     */
    List<WeeklyProductRank> findByYearWeekOrderByRankPositionAsc(String yearWeek, Pageable pageable);
}
