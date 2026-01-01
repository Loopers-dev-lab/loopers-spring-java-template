package com.loopers.infrastructure.ranking;

import com.loopers.batch.domain.ranking.MonthlyProductRank;
import com.loopers.batch.domain.ranking.MonthlyProductRankId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyProductRankJpaRepository extends JpaRepository<MonthlyProductRank, MonthlyProductRankId> {
}
