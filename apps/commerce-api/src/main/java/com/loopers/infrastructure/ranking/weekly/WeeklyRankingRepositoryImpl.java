package com.loopers.infrastructure.ranking.weekly;

import com.loopers.domain.ranking.weekly.WeeklyRanking;
import com.loopers.domain.ranking.weekly.WeeklyRankingRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.loopers.domain.ranking.weekly.QWeeklyRanking.weeklyRanking;

@Repository
@RequiredArgsConstructor
public class WeeklyRankingRepositoryImpl implements WeeklyRankingRepository {

    private final WeeklyRankingJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<WeeklyRanking> findByWeekStartDateOrderByRankPosition(LocalDate weekStartDate) {
        return queryFactory
            .selectFrom(weeklyRanking)
            .where(weeklyRanking.weekStartDate.eq(weekStartDate))
            .orderBy(weeklyRanking.rankPosition.asc())
            .fetch();
    }

    @Override
    public List<WeeklyRanking> findByWeekStartDateWithPagination(LocalDate weekStartDate, int offset, int limit) {
        return queryFactory
            .selectFrom(weeklyRanking)
            .where(weeklyRanking.weekStartDate.eq(weekStartDate))
            .orderBy(weeklyRanking.rankPosition.asc())
            .offset(offset)
            .limit(limit)
            .fetch();
    }

    @Override
    @Transactional
    public void deleteByWeekStartDate(LocalDate weekStartDate) {
        queryFactory
            .delete(weeklyRanking)
            .where(weeklyRanking.weekStartDate.eq(weekStartDate))
            .execute();
    }

    @Override
    @Transactional
    public void saveAll(List<WeeklyRanking> rankings) {
        jpaRepository.saveAll(rankings);
    }
}