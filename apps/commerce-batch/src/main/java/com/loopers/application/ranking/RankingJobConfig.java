package com.loopers.application.ranking;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsService;
import com.loopers.domain.ranking.MonthlyRankingService;
import com.loopers.domain.ranking.ProductRankingAggregate;
import com.loopers.domain.ranking.RankingCalculator;
import com.loopers.domain.ranking.WeeklyRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductMetricsService productMetricsService;
    private final RankingCalculator rankingCalculator;
    private final RankingCalculationTasklet rankingCalculationTasklet;
    private final WeeklyRankingService weeklyRankingService;
    private final MonthlyRankingService monthlyRankingService;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job rankingJob() {
        return new JobBuilder("rankingJob", jobRepository)
                .start(rankingStep())
                .next(rankingCalculationStep())
                .build();
    }

    @Bean
    public Step rankingStep() {
        return new StepBuilder("rankingStep", jobRepository)
                .<ProductMetrics, ProductRankingAggregate>chunk(CHUNK_SIZE, transactionManager)
                .reader(rankingReader())
                .processor(rankingProcessor())
                .writer(rankingWriter())
                .build();
    }

    @Bean
    public Step rankingCalculationStep() {
        return new StepBuilder("rankingCalculationStep", jobRepository)
                .tasklet(rankingCalculationTasklet, transactionManager)
                .build();
    }

    @Bean
    public ItemReader<ProductMetrics> rankingReader() {
        return new ProductMetricsItemReader(productMetricsService);
    }

    @Bean
    public ItemProcessor<ProductMetrics, ProductRankingAggregate> rankingProcessor() {
        return new RankingProcessor(rankingCalculator);
    }

    @Bean
    public ItemWriter<ProductRankingAggregate> rankingWriter() {
        return new RankingWriter(weeklyRankingService, monthlyRankingService);
    }
}

