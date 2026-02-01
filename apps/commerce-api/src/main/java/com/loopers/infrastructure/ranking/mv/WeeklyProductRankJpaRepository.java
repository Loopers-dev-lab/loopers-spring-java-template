package com.loopers.infrastructure.ranking.mv;

import com.loopers.domain.ranking.mv.WeeklyProductRank;
import com.loopers.domain.ranking.mv.WeeklyProductRankId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyProductRankJpaRepository extends JpaRepository<WeeklyProductRank, WeeklyProductRankId> {

  List<WeeklyProductRank> findByYearWeekOrderByScoreDesc(String yearWeek, Pageable pageable);
}
