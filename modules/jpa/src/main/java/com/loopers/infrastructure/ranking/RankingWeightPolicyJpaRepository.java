package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingWeightPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RankingWeightPolicyJpaRepository extends JpaRepository<RankingWeightPolicy, Long> {
    Optional<RankingWeightPolicy> findByEventType(RankingEventType eventType);
    List<RankingWeightPolicy> findByIsActiveTrue();
}

