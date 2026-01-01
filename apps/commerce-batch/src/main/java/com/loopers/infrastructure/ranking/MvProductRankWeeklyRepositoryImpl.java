package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.domain.ranking.MvProductRankWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MvProductRankWeeklyRepositoryImpl implements MvProductRankWeeklyRepository {

    private final MvProductRankWeeklyJpaRepository jpaRepository;

    @Override
    public Optional<MvProductRankWeekly> findByProductIdAndPeriod(Long productId, LocalDate periodStartDate, LocalDate periodEndDate) {
        return jpaRepository.findByProductIdAndPeriodStartDateAndPeriodEndDate(productId, periodStartDate, periodEndDate);
    }

    @Override
    public List<MvProductRankWeekly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate) {
        return jpaRepository.findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(periodStartDate, periodEndDate);
    }

    @Override
    public void save(MvProductRankWeekly rank) {
        jpaRepository.save(rank);
    }

    @Override
    public void delete(MvProductRankWeekly rank) {
        jpaRepository.delete(rank);
    }
}

