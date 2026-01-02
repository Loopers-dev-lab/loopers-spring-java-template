package com.loopers.infrastructure.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface ProductRankWeeklyRepository extends JpaRepository<ProductRankWeekly, Long> {

    List<ProductRankWeekly> findByPeriodStartOrderByRankingAsc(LocalDate periodStart);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductRankWeekly p WHERE p.periodStart = :periodStart")
    void deleteByPeriodStart(@Param("periodStart") LocalDate periodStart);
}
