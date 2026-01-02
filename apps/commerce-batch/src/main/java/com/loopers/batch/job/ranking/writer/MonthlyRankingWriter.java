package com.loopers.batch.job.ranking.writer;


import com.loopers.batch.domain.ranking.MonthlyRanking;
import com.loopers.batch.domain.ranking.MonthlyRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class MonthlyRankingWriter implements ItemWriter<MonthlyRanking> {

    private final MonthlyRankingRepository monthlyRankingRepository;

    @Value("#{jobParameters['yearMonth']}")
    private String yearMonthStr;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private boolean deletedExisting = false;

    @Override
    @Transactional
    public void write(Chunk<? extends MonthlyRanking> chunk) {
        if (!deletedExisting) {
            YearMonth yearMonth = parseYearMonth();

            log.info("기존 월간 랭킹 삭제: yearMonth={}", yearMonth);
            monthlyRankingRepository.deleteByMonthPeriod(yearMonth);
            deletedExisting = true;
        }

        log.info("월간 랭킹 저장: count={}", chunk.size());
        monthlyRankingRepository.saveAll(chunk.getItems().stream().map(item -> (MonthlyRanking) item).toList());
    }

    private YearMonth parseYearMonth() {
        if (yearMonthStr != null && !yearMonthStr.isBlank()) {
            return YearMonth.parse(yearMonthStr, YEAR_MONTH_FORMATTER);
        }
        return YearMonth.now();
    }
}
