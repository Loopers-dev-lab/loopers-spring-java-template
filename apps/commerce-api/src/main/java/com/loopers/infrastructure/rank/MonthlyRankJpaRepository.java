package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.MonthlyProductRank;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA Repository for MonthlyProductRank entity.
 */
public interface MonthlyRankJpaRepository extends JpaRepository<MonthlyProductRank, Long> {

    /**
     * Finds monthly rankings for a specific month with pagination.
     *
     * @param yearMonth the year-month (e.g., "2025-01")
     * @param pageable pagination parameters
     * @return list of monthly rankings
     */
    List<MonthlyProductRank> findByYearMonthOrderByRankPositionAsc(String yearMonth, Pageable pageable);
}
