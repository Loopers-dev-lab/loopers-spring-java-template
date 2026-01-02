package com.loopers.infrastructure.ranking;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;

import lombok.RequiredArgsConstructor;

/**
 * 월간 랭킹 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class MonthlyRankRepositoryImpl implements MonthlyRankRepository {

    private final MonthlyRankJpaRepository jpaRepository;

    @Override
    public MonthlyRankEntity save(MonthlyRankEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<MonthlyRankEntity> saveAll(List<MonthlyRankEntity> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Page<MonthlyRankEntity> findByYearMonth(String yearMonth, Pageable pageable) {
        return jpaRepository.findByIdYearMonthOrderByRankPosition(yearMonth, pageable);
    }

    @Override
    public long deleteByYearMonth(String yearMonth) {
        return jpaRepository.deleteByIdYearMonth(yearMonth);
    }
}
