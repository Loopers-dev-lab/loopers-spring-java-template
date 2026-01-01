package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankMonthlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MvProductRankMonthlyRepositoryImpl implements MvProductRankMonthlyRepository {

    private final MvProductRankMonthlyJpaRepository jpaRepository;

    @Override
    public Optional<MvProductRankMonthly> findByProductIdAndPeriod(Long productId, LocalDate periodStartDate, LocalDate periodEndDate) {
        return jpaRepository.findByProductIdAndPeriodStartDateAndPeriodEndDate(productId, periodStartDate, periodEndDate);
    }

    @Override
    public List<MvProductRankMonthly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate) {
        return jpaRepository.findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(periodStartDate, periodEndDate);
    }

    @Override
    public void save(MvProductRankMonthly rank) {
        jpaRepository.save(rank);
    }

    @Override
    public void delete(MvProductRankMonthly rank) {
        jpaRepository.delete(rank);
    }
}

