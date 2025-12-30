package com.loopers.domain.ranking;

import java.util.List;
import java.util.Optional;

public interface RankingWeightPolicyRepository {
    Optional<RankingWeightPolicy> findByEventType(RankingEventType eventType);
    List<RankingWeightPolicy> findAllActive();
    RankingWeightPolicy save(RankingWeightPolicy policy);
}

