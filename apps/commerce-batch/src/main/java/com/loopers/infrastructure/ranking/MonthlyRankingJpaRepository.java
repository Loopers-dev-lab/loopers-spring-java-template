package com.loopers.infrastructure.ranking;

import com.loopers.batch.domain.ranking.MonthlyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

public interface MonthlyRankingJpaRepository extends JpaRepository<MonthlyRanking, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM MonthlyRanking m WHERE m.monthPeriod = :monthPeriod")
    void deleteByMonthPeriod(@Param("monthPeriod") YearMonth monthPeriod);
}
