package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.dto.QWeeklyProductRankingProjection;
import com.loopers.core.infra.database.mysql.product.dto.WeeklyProductRankingProjection;
import com.loopers.core.infra.database.mysql.product.entity.QWeeklyProductMetricEntity;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WeeklyProductMetricQuerydslRepositoryImpl implements WeeklyProductMetricQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<WeeklyProductRankingProjection> findWeeklyProductRanking(
            LocalDate date,
            Double payWeight,
            Double viewWeight,
            Double likeWeight,
            Pageable pageable
    ) {
        QWeeklyProductMetricEntity metric = QWeeklyProductMetricEntity.weeklyProductMetricEntity;

        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int year = date.getYear();
        int month = date.getMonthValue();
        int weekOfYear = date.get(weekFields.weekOfYear());

        NumberExpression<Double> scoreCalculation = metric.totalSalesCount.doubleValue().multiply(payWeight)
                .add(metric.viewCount.doubleValue().multiply(viewWeight))
                .add(metric.likeCount.doubleValue().multiply(likeWeight));

        List<WeeklyProductRankingProjection> content = queryFactory
                .select(new QWeeklyProductRankingProjection(
                        metric.productId,
                        Expressions.numberTemplate(Long.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)",
                                scoreCalculation),
                        scoreCalculation.as("score")
                ))
                .from(metric)
                .where(metric.year.eq(year)
                        .and(metric.month.eq(month))
                        .and(metric.weekOfYear.eq(weekOfYear)))
                .orderBy(scoreCalculation.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(metric.count())
                .from(metric)
                .where(metric.year.eq(year)
                        .and(metric.month.eq(month))
                        .and(metric.weekOfYear.eq(weekOfYear)));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
