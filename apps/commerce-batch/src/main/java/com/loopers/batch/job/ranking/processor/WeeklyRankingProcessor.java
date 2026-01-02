package com.loopers.batch.job.ranking.processor;

import com.loopers.batch.domain.ranking.WeeklyRanking;
import com.loopers.dto.RankedProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@StepScope
@Component
public class WeeklyRankingProcessor implements ItemProcessor<RankedProduct, WeeklyRanking> {

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicInteger rankCounter = new AtomicInteger(0);

    @Override
    public WeeklyRanking process(RankedProduct item) {
        LocalDate weekStart = parseStartDate();
        LocalDate weekEnd = parseEndDate();

        int rank = rankCounter.incrementAndGet();

        return WeeklyRanking.create(
                rank,
                item.productId(),
                item.score(),
                weekStart,
                weekEnd
        );
    }

    private LocalDate parseStartDate() {
        if (startDateStr != null && !startDateStr.isBlank()) {
            return LocalDate.parse(startDateStr, DATE_FORMATTER);
        }
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private LocalDate parseEndDate() {
        if (endDateStr != null && !endDateStr.isBlank()) {
            return LocalDate.parse(endDateStr, DATE_FORMATTER);
        }
        return LocalDate.now().with(DayOfWeek.SUNDAY);
    }
}
