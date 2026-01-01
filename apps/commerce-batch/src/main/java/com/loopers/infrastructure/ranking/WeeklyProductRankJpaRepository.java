package com.loopers.infrastructure.ranking;

import com.loopers.batch.domain.ranking.WeeklyProductRank;
import com.loopers.batch.domain.ranking.WeeklyProductRankId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyProductRankJpaRepository extends JpaRepository<WeeklyProductRank, WeeklyProductRankId> {
}
