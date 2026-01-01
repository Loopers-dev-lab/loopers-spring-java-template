package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.domain.ranking.MvProductRankWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MvProductRankWeeklyRepositoryImpl implements MvProductRankWeeklyRepository {
    private final MvProductRankWeeklyJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MvProductRankWeekly> findTop100ByRankingDateOrderByRankAsc(ZonedDateTime rankingDate) {
        return jpaRepository.findTop100ByRankingDateOrderByRankAsc(rankingDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MvProductRankWeekly> findByProductIdAndRankingDate(Long productId, ZonedDateTime rankingDate) {
        return jpaRepository.findByProductIdAndRankingDate(productId, rankingDate);
    }

    @Override
    @Transactional
    public void deleteByRankingDate(ZonedDateTime rankingDate) {
        jpaRepository.deleteByRankingDate(rankingDate);
    }

    @Override
    @Transactional
    public void saveAll(List<MvProductRankWeekly> rankings) {
        jpaRepository.saveAll(rankings);
    }
}
