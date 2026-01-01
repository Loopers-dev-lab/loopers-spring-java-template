package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankMonthlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MvProductRankMonthlyRepositoryImpl implements MvProductRankMonthlyRepository {

    private final MvProductRankMonthlyJpaRepository mvProductRankMonthlyJpaRepository;

    @Override
    public List<MvProductRankMonthly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return mvProductRankMonthlyJpaRepository.findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(periodStartDate, periodEndDate, pageable);
    }
}

