package com.loopers.application.ranking;

import com.loopers.domain.ranking.MonthlyRankingService;
import com.loopers.domain.ranking.ProductRankingAggregate;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.RankingType;
import com.loopers.domain.ranking.WeeklyRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
public class RankingWriter implements ItemWriter<ProductRankingAggregate> {

    private final WeeklyRankingService weeklyRankingService;
    private final MonthlyRankingService monthlyRankingService;

    private RankingPeriod period;
    private RankingType rankingType;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        LocalDate targetDate = JobParameterUtils.getTargetDate(stepExecution);
        this.rankingType = JobParameterUtils.getRankingType(stepExecution);
        
        if (rankingType == RankingType.WEEKLY) {
            this.period = RankingPeriod.ofWeek(targetDate);
        } else {
            this.period = RankingPeriod.ofMonth(targetDate);
        }
    }

    @Override
    public void write(Chunk<? extends ProductRankingAggregate> items) throws Exception {
        for (ProductRankingAggregate aggregate : items) {
            Long productId = aggregate.getProductId();
            
            if (rankingType == RankingType.WEEKLY) {
                weeklyRankingService.upsertMetrics(
                        productId,
                        period,
                        aggregate.getScore(),
                        aggregate.getTotalLikeCount(),
                        aggregate.getTotalViewCount(),
                        aggregate.getTotalSalesCount()
                );
            } else {
                monthlyRankingService.upsertMetrics(
                        productId,
                        period,
                        aggregate.getScore(),
                        aggregate.getTotalLikeCount(),
                        aggregate.getTotalViewCount(),
                        aggregate.getTotalSalesCount()
                );
            }
        }
    }
}
