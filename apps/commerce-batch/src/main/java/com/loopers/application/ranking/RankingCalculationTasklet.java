package com.loopers.application.ranking;

import com.loopers.domain.ranking.MonthlyRankingService;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.RankingType;
import com.loopers.domain.ranking.WeeklyRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingCalculationTasklet implements Tasklet {

    private final WeeklyRankingService weeklyRankingService;
    private final MonthlyRankingService monthlyRankingService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var stepExecution = chunkContext.getStepContext().getStepExecution();

        LocalDate targetDate = JobParameterUtils.getTargetDate(stepExecution);
        RankingType rankingType = JobParameterUtils.getRankingType(stepExecution);
        RankingPeriod period;

        log.info("랭킹 계산 시작: rankingType={}, targetDate={}", rankingType, targetDate);

        if (rankingType == RankingType.WEEKLY) {
            period = RankingPeriod.ofWeek(targetDate);
            weeklyRankingService.calculateAndUpdateRanking(period);
            log.info("주간 랭킹 계산 완료: targetDate={}, period={} ~ {}", targetDate, period.startDate(), period.endDate());
        } else {
            period = RankingPeriod.ofMonth(targetDate);
            monthlyRankingService.calculateAndUpdateRanking(period);
            log.info("월간 랭킹 계산 완료: targetDate={}, period={} ~ {}", targetDate, period.startDate(), period.endDate());
        }

        return RepeatStatus.FINISHED;
    }
}

