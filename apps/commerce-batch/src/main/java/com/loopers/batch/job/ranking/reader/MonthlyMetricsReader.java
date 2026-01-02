package com.loopers.batch.job.ranking.reader;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.loopers.batch.job.ranking.support.DateRangeParser;
import com.loopers.batch.job.ranking.support.RankingAggregator;
import com.loopers.domain.metrics.ProductMetricsRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 월간 메트릭 Reader
 * - 지정된 월의 전체 일간 데이터를 집계
 * - TOP 100 랭킹 생성 및 순위 부여
 */
@Slf4j
@StepScope
@Component
public class MonthlyMetricsReader extends AbstractMetricsReader {

    private final DateRangeParser dateRangeParser;

    @Value("#{jobParameters['yearMonth']}")
    private String yearMonth;  // e.g., "2024-12"

    public MonthlyMetricsReader(
            ProductMetricsRepository productMetricsRepository,
            RankingAggregator rankingAggregator,
            DateRangeParser dateRangeParser) {
        super(productMetricsRepository, rankingAggregator);
        this.dateRangeParser = dateRangeParser;
    }

    @Override
    protected LocalDate[] parseDateRange() {
        return dateRangeParser.parseYearMonth(yearMonth);
    }

    @Override
    protected String getLogIdentifier() {
        return "월간";
    }

    @Override
    protected String getParameterValue() {
        return yearMonth;
    }
}
