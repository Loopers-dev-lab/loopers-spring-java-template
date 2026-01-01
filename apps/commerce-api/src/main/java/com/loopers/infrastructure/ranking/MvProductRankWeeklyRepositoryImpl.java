package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.domain.ranking.MvProductRankWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MvProductRankWeeklyRepositoryImpl implements MvProductRankWeeklyRepository {

    private final MvProductRankWeeklyJpaRepository mvProductRankWeeklyJpaRepository;

    @Override
    public List<MvProductRankWeekly> findByPeriodOrderByRankingAsc(LocalDate periodStartDate, LocalDate periodEndDate, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return mvProductRankWeeklyJpaRepository.findByPeriodStartDateAndPeriodEndDateOrderByRankingAsc(periodStartDate, periodEndDate, pageable);
    }
}

