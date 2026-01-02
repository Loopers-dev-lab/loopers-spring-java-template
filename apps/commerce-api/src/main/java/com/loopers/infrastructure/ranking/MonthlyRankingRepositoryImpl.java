package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyRankingMv;
import com.loopers.domain.ranking.MonthlyRankingRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.loopers.domain.ranking.QMonthlyRankingMv.monthlyRankingMv;

@Component
@RequiredArgsConstructor
public class MonthlyRankingRepositoryImpl implements MonthlyRankingRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MonthlyRankingMv> getMonthlyTop100(String endDate, String startDate, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        long offset = (long) (safePage - 1) * safeSize;

        return queryFactory
                .selectFrom(monthlyRankingMv)
                .where(
                        monthlyRankingMv.startDate.eq(startDate)
                                .and(monthlyRankingMv.endDate.eq(endDate)))
                .orderBy(
                        monthlyRankingMv.score.desc(),
                        monthlyRankingMv.productId.asc()
                )
                .offset(offset)
                .limit(safeSize)
                .fetch();
    }

    @Override
    public long countMonthly(String startDate, String endDate) {
        Long cnt = queryFactory
                .select(monthlyRankingMv.count())
                .from(monthlyRankingMv)
                .where(
                        monthlyRankingMv.startDate.eq(startDate)
                                .and(monthlyRankingMv.endDate.eq(endDate))
                )
                .fetchOne();

        return cnt == null ? 0L : cnt;
    }
}
