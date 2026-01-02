package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyRankingMv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyRankingJpaRepository extends JpaRepository<MonthlyRankingMv, Long> {
}
