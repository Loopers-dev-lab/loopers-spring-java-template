package com.loopers.batch.job.ranking.writer;

import com.loopers.batch.domain.ranking.WeeklyRanking;
import com.loopers.batch.domain.ranking.WeeklyRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class WeeklyRankingWriter implements ItemWriter<WeeklyRanking> {

    private final WeeklyRankingRepository weeklyRankingRepository;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private boolean deletedExisting = false;

    @Override
    @Transactional
    public void write(Chunk<? extends WeeklyRanking> chunk) {
        if (!deletedExisting) {
            LocalDate weekStart = parseStartDate();
            LocalDate weekEnd = parseEndDate();

            log.info("기존 주간 랭킹 삭제: weekStart={}, weekEnd={}", weekStart, weekEnd);
            weeklyRankingRepository.deleteByWeekStartAndWeekEnd(weekStart, weekEnd);
            deletedExisting = true;
        }

        log.info("주간 랭킹 저장: count={}", chunk.size());
        weeklyRankingRepository.saveAll(chunk.getItems().stream().map(item -> (WeeklyRanking) item).toList());
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
