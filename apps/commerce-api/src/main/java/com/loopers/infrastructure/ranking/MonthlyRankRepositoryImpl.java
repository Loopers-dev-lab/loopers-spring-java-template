package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 월간 랭킹 Repository 구현체 (commerce-api용)
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
    public List<MonthlyRankEntity> findByYearMonth(String yearMonth) {
        return jpaRepository.findByIdYearMonthOrderByRankPosition(yearMonth);
    }

    @Override
    public List<MonthlyRankEntity> findByYearMonthWithPagination(String yearMonth, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByIdYearMonthOrderByRankPosition(yearMonth, pageable);
    }

    @Override
    public long deleteByYearMonth(String yearMonth) {
        return jpaRepository.deleteByIdYearMonth(yearMonth);
    }
}