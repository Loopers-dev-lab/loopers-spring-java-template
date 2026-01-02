package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankMonthlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MvProductRankMonthlyRepositoryImpl implements MvProductRankMonthlyRepository {
    private final MvProductRankMonthlyJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MvProductRankMonthly> findTop100ByRankingDateOrderByRankingAsc(ZonedDateTime rankingDate) {
        return jpaRepository.findTop100ByRankingDateOrderByRankingAsc(rankingDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MvProductRankMonthly> findByProductIdAndRankingDate(Long productId, ZonedDateTime rankingDate) {
        return jpaRepository.findByProductIdAndRankingDate(productId, rankingDate);
    }

    @Override
    @Transactional
    public void deleteByRankingDate(ZonedDateTime rankingDate) {
        jpaRepository.deleteByRankingDate(rankingDate);
    }

    @Override
    @Transactional
    public void saveAll(List<MvProductRankMonthly> rankings) {
        jpaRepository.saveAll(rankings);
    }
}
