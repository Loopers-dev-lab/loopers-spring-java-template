package com.loopers.batch.config;

import com.loopers.batch.processor.RankingScoreProcessor;
import com.loopers.batch.reader.ProductMetricsAggregateReader;
import com.loopers.batch.writer.MonthlyRankingWriter;
import com.loopers.domain.dto.ProductRankingAggregation;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.MonthlyRankRepository;
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
 * Configuration class for monthly ranking aggregation batch job.
 *
 * <p>This job aggregates product metrics data on a monthly basis and
 * stores the top N rankings in the materialized view table.
 *
 * <p>Job execution example:
 * <pre>
 * java -jar commerce-batch.jar \
 *   --job.name=monthlyRankingJob \
 *   yearMonth=2025-01
 * </pre>
 *
 * <p>Chunk-oriented processing flow:
 * <ol>
 *   <li>Reader: Aggregate product_metrics by month</li>
 *   <li>Processor: Calculate ranking scores</li>
 *   <li>Writer: Save to mv_product_rank_monthly</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;

    private final EntityManager entityManager;
    private final MonthlyRankRepository monthlyRankRepository;

    /**
     * Defines the monthly ranking job.
     *
     * @param jobRepository the Spring Batch job repository
     * @param monthlyRankingStep the step to execute
     * @return configured Job instance
     */
    @Bean
    public Job monthlyRankingJob(
        JobRepository jobRepository,
        Step monthlyRankingStep
    ) {
        return new JobBuilder("monthlyRankingJob", jobRepository)
            .start(monthlyRankingStep)
            .build();
    }

    /**
     * Defines the monthly ranking step with chunk-oriented processing.
     *
     * @param jobRepository the Spring Batch job repository
     * @param transactionManager the transaction manager
     * @param yearMonth the target month (injected from job parameters)
     * @return configured Step instance
     */
    @Bean
    @JobScope
    public Step monthlyRankingStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        @Value("#{jobParameters['yearMonth']}") String yearMonth
    ) {
        log.info("Initializing monthly ranking step: yearMonth={}", yearMonth);

        return new StepBuilder("monthlyRankingStep", jobRepository)
            .<ProductRankingAggregation, MonthlyProductRank>chunk(CHUNK_SIZE, transactionManager)
            .reader(monthlyMetricsReader(yearMonth))
            .processor(monthlyRankingProcessor(yearMonth))
            .writer(monthlyRankingWriter())
            .build();
    }

    /**
     * Creates an ItemReader for monthly metrics aggregation.
     *
     * @param yearMonth the target month
     * @return configured ItemReader
     */
    @Bean
    @StepScope
    public ItemReader<ProductRankingAggregation> monthlyMetricsReader(
        @Value("#{jobParameters['yearMonth']}") String yearMonth
    ) {
        return new ProductMetricsAggregateReader(entityManager, yearMonth, "MONTHLY", TOP_N);
    }

    /**
     * Creates an ItemProcessor for ranking score calculation.
     *
     * @param yearMonth the target month
     * @return configured ItemProcessor
     */
    @Bean
    @StepScope
    public ItemProcessor<ProductRankingAggregation, MonthlyProductRank> monthlyRankingProcessor(
        @Value("#{jobParameters['yearMonth']}") String yearMonth
    ) {
        RankingScoreProcessor processor = new RankingScoreProcessor("MONTHLY", yearMonth);
        return item -> (MonthlyProductRank) processor.process(item);
    }

    /**
     * Creates an ItemWriter for persisting monthly rankings.
     *
     * @return configured ItemWriter
     */
    @Bean
    @StepScope
    public ItemWriter<MonthlyProductRank> monthlyRankingWriter() {
        return new MonthlyRankingWriter(monthlyRankRepository);
    }
}
