package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.MonthlyProductRank;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyRankJpaRepository extends JpaRepository<MonthlyProductRank, Long> {
	List<MonthlyProductRank> findByPeriodStartOrderByRankPositionAsc(LocalDate periodStart, Pageable pageable);
}


