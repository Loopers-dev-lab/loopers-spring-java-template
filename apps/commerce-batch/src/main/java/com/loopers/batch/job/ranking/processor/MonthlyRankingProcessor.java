package com.loopers.batch.job.ranking.processor;

import com.loopers.batch.domain.ranking.MonthlyRanking;
import com.loopers.dto.RankedProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@StepScope
@Component
public class MonthlyRankingProcessor implements ItemProcessor<RankedProduct, MonthlyRanking> {

    @Value("#{jobParameters['yearMonth']}")
    private String yearMonthStr;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AtomicInteger rankCounter = new AtomicInteger(0);

    @Override
    public MonthlyRanking process(RankedProduct item) {
        YearMonth yearMonth = parseYearMonth();

        int rank = rankCounter.incrementAndGet();

        return MonthlyRanking.create(
                rank,
                item.productId(),
                item.score(),
                yearMonth
        );
    }

    private YearMonth parseYearMonth() {
        if (yearMonthStr != null && !yearMonthStr.isBlank()) {
            return YearMonth.parse(yearMonthStr, YEAR_MONTH_FORMATTER);
        }
        return YearMonth.now();
    }
}
