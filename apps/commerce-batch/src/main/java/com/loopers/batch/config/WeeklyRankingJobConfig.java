package com.loopers.batch.config;

import com.loopers.batch.processor.RankingScoreProcessor;
import com.loopers.batch.reader.ProductMetricsAggregateReader;
import com.loopers.batch.writer.WeeklyRankingWriter;
import com.loopers.domain.dto.ProductRankingAggregation;
import com.loopers.domain.rank.WeeklyProductRank;
import com.loopers.domain.rank.WeeklyRankRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration class for weekly ranking aggregation batch job.
 *
 * <p>This job aggregates product metrics data on a weekly basis and
 * stores the top N rankings in the materialized view table.
 *
 * <p>Job execution example:
 * <pre>
 * java -jar commerce-batch.jar \
 *   --job.name=weeklyRankingJob \
 *   yearWeek=2025-W01
 * </pre>
 *
 * <p>Chunk-oriented processing flow:
 * <ol>
 *   <li>Reader: Aggregate product_metrics by week</li>
 *   <li>Processor: Calculate ranking scores</li>
 *   <li>Writer: Save to mv_product_rank_weekly</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklyRankingJobConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;

    private final EntityManager entityManager;
    private final WeeklyRankRepository weeklyRankRepository;

    /**
     * Defines the weekly ranking job.
     *
     * @param jobRepository the Spring Batch job repository
     * @param weeklyRankingStep the step to execute
     * @return configured Job instance
     */
    @Bean
    public Job weeklyRankingJob(
        JobRepository jobRepository,
        Step weeklyRankingStep
    ) {
        return new JobBuilder("weeklyRankingJob", jobRepository)
            .start(weeklyRankingStep)
            .build();
    }

    /**
     * Defines the weekly ranking step with chunk-oriented processing.
     *
     * @param jobRepository the Spring Batch job repository
     * @param transactionManager the transaction manager
     * @param yearWeek the target week in ISO format (injected from job parameters)
     * @return configured Step instance
     */
    @Bean
    @JobScope
    public Step weeklyRankingStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        @Value("#{jobParameters['yearWeek']}") String yearWeek
    ) {
        log.info("Initializing weekly ranking step: yearWeek={}", yearWeek);

        return new StepBuilder("weeklyRankingStep", jobRepository)
            .<ProductRankingAggregation, WeeklyProductRank>chunk(CHUNK_SIZE, transactionManager)
            .reader(weeklyMetricsReader(yearWeek))
            .processor(weeklyRankingProcessor(yearWeek))
            .writer(weeklyRankingWriter())
            .build();
    }

    /**
     * Creates an ItemReader for weekly metrics aggregation.
     *
     * @param yearWeek the target week
     * @return configured ItemReader
     */
    @Bean
    @StepScope
    public ItemReader<ProductRankingAggregation> weeklyMetricsReader(
        @Value("#{jobParameters['yearWeek']}") String yearWeek
    ) {
        return new ProductMetricsAggregateReader(entityManager, yearWeek, "WEEKLY", TOP_N);
    }

    /**
     * Creates an ItemProcessor for ranking score calculation.
     *
     * @param yearWeek the target week
     * @return configured ItemProcessor
     */
    @Bean
    @StepScope
    public ItemProcessor<ProductRankingAggregation, WeeklyProductRank> weeklyRankingProcessor(
        @Value("#{jobParameters['yearWeek']}") String yearWeek
    ) {
        RankingScoreProcessor processor = new RankingScoreProcessor("WEEKLY", yearWeek);
        return item -> (WeeklyProductRank) processor.process(item);
    }

    /**
     * Creates an ItemWriter for persisting weekly rankings.
     *
     * @return configured ItemWriter
     */
    @Bean
    @StepScope
    public ItemWriter<WeeklyProductRank> weeklyRankingWriter() {
        return new WeeklyRankingWriter(weeklyRankRepository);
    }
}
