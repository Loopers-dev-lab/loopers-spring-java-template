package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.WeeklyRankingMv;
import com.loopers.domain.ranking.WeeklyRankingRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.loopers.domain.ranking.QWeeklyRankingMv.weeklyRankingMv;

@Component
@RequiredArgsConstructor
public class WeeklyRankingRepositoryImpl implements WeeklyRankingRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<WeeklyRankingMv> getWeeklyTop100(String endDate, String startDate, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        long offset = (long) (safePage - 1) * safeSize;

        return queryFactory
                .selectFrom(weeklyRankingMv)
                .where(
                        weeklyRankingMv.startDate.eq(startDate)
                                .and(weeklyRankingMv.endDate.eq(endDate)))
                .orderBy(
                        weeklyRankingMv.score.desc(),
                        weeklyRankingMv.productId.asc()
                )
                .offset(offset)
                .limit(safeSize)
                .fetch();
    }

    @Override
    public long countWeekly(String startDate, String endDate) {
        Long cnt = queryFactory
                .select(weeklyRankingMv.count())
                .from(weeklyRankingMv)
                .where(
                        weeklyRankingMv.startDate.eq(startDate)
                                .and(weeklyRankingMv.endDate.eq(endDate))
                )
                .fetchOne();

        return cnt == null ? 0L : cnt;
    }
}
