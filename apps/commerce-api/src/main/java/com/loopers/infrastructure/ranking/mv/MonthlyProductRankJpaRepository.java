package com.loopers.infrastructure.ranking.mv;

import com.loopers.domain.ranking.mv.MonthlyProductRank;
import com.loopers.domain.ranking.mv.MonthlyProductRankId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyProductRankJpaRepository extends JpaRepository<MonthlyProductRank, MonthlyProductRankId> {

  List<MonthlyProductRank> findByYearMonthOrderByScoreDesc(String yearMonth, Pageable pageable);
}
