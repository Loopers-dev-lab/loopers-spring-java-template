package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingWeightPolicy;
import com.loopers.domain.ranking.RankingWeightPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RankingWeightPolicyRepositoryImpl implements RankingWeightPolicyRepository {

    private final RankingWeightPolicyJpaRepository jpaRepository;

    @Override
    public Optional<RankingWeightPolicy> findByEventType(RankingEventType eventType) {
        return jpaRepository.findByEventType(eventType);
    }

    @Override
    public List<RankingWeightPolicy> findAllActive() {
        return jpaRepository.findByIsActiveTrue();
    }

    @Override
    public RankingWeightPolicy save(RankingWeightPolicy policy) {
        return jpaRepository.save(policy);
    }
}

