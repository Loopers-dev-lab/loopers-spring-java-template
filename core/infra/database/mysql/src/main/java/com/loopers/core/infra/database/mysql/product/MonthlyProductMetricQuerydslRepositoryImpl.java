package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.dto.QMonthlyProductRankingProjection;
import com.loopers.core.infra.database.mysql.product.dto.MonthlyProductRankingProjection;
import com.loopers.core.infra.database.mysql.product.entity.QMonthlyProductMetricEntity;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyProductMetricQuerydslRepositoryImpl implements MonthlyProductMetricQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MonthlyProductRankingProjection> findMonthlyProductRanking(
            LocalDate date,
            Double payWeight,
            Double viewWeight,
            Double likeWeight,
            Pageable pageable
    ) {
        QMonthlyProductMetricEntity metric = QMonthlyProductMetricEntity.monthlyProductMetricEntity;

        int year = date.getYear();
        int month = date.getMonthValue();

        NumberExpression<Double> scoreCalculation = metric.totalSalesCount.doubleValue().multiply(payWeight)
                .add(metric.viewCount.doubleValue().multiply(viewWeight))
                .add(metric.likeCount.doubleValue().multiply(likeWeight));

        List<MonthlyProductRankingProjection> content = queryFactory
                .select(new QMonthlyProductRankingProjection(
                        metric.productId,
                        Expressions.numberTemplate(Long.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)",
                                scoreCalculation),
                        scoreCalculation.as("score")
                ))
                .from(metric)
                .where(metric.year.eq(year)
                        .and(metric.month.eq(month)))
                .orderBy(scoreCalculation.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(metric.count())
                .from(metric)
                .where(metric.year.eq(year)
                        .and(metric.month.eq(month)));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
