package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.WeeklyProductRankView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyRankJpaRepository extends JpaRepository<WeeklyProductRankView, Long> {
	List<WeeklyProductRankView> findByPeriodStartOrderByRankPositionAsc(LocalDate periodStart, Pageable pageable);
}


