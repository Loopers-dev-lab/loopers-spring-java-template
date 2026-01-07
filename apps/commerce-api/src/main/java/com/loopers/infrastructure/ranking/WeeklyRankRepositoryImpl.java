package com.loopers.infrastructure.ranking;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주간 랭킹 Repository 구현체 (commerce-api용)
 */
@Repository
@RequiredArgsConstructor
public class WeeklyRankRepositoryImpl implements WeeklyRankRepository {

    private final WeeklyRankJpaRepository jpaRepository;

    @Override
    public WeeklyRankEntity save(WeeklyRankEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<WeeklyRankEntity> saveAll(List<WeeklyRankEntity> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public List<WeeklyRankEntity> findByYearWeek(String yearWeek) {
        return jpaRepository.findByIdYearWeekOrderByRankPosition(yearWeek);
    }
    @Override
    public long deleteByYearWeek(String yearWeek) {
        return jpaRepository.deleteByIdYearWeek(yearWeek);
    }

    @Override
    public Page<WeeklyRankEntity> findByYearWeek(String yearWeek, Pageable pageable) {
        return jpaRepository.findByIdYearWeekOrderByRankPosition(yearWeek, pageable);
    }
}
