package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.WeeklyRankingMv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyRankingJpaRepository extends JpaRepository<WeeklyRankingMv, Long> {

}
