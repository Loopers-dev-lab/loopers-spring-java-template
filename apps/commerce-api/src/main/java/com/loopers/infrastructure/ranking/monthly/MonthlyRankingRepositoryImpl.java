package com.loopers.infrastructure.ranking.monthly;

import com.loopers.domain.ranking.monthly.MonthlyRanking;
import com.loopers.domain.ranking.monthly.MonthlyRankingRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.loopers.domain.ranking.monthly.QMonthlyRanking.monthlyRanking;

@Repository
@RequiredArgsConstructor
public class MonthlyRankingRepositoryImpl implements MonthlyRankingRepository {
    
    private final MonthlyRankingJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<MonthlyRanking> findByMonthYearOrderByRankPosition(String monthYear) {
        return queryFactory
            .selectFrom(monthlyRanking)
            .where(monthlyRanking.monthYear.eq(monthYear))
            .orderBy(monthlyRanking.rankPosition.asc())
            .fetch();
    }
    
    @Override
    public List<MonthlyRanking> findByMonthYearWithPagination(String monthYear, int offset, int limit) {
        return queryFactory
            .selectFrom(monthlyRanking)
            .where(monthlyRanking.monthYear.eq(monthYear))
            .orderBy(monthlyRanking.rankPosition.asc())
            .offset(offset)
            .limit(limit)
            .fetch();
    }
    
    @Override
    @Transactional
    public void deleteByMonthYear(String monthYear) {
        queryFactory
            .delete(monthlyRanking)
            .where(monthlyRanking.monthYear.eq(monthYear))
            .execute();
    }

    @Override
    @Transactional
    public void saveAll(List<MonthlyRanking> rankings) {
        jpaRepository.saveAll(rankings);
    }
}