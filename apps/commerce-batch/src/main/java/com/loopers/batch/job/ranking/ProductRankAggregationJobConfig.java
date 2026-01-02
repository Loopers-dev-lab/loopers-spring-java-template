package com.loopers.batch.job.ranking;

import com.loopers.batch.domain.metrics.ProductMetrics;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = ProductRankAggregationJobConfig.JOB_NAME)
public class ProductRankAggregationJobConfig {

    public static final String JOB_NAME = "productRankAggregationJob";
    private static final String STEP_WEEKLY = "weeklyProductRankingStep";
    private static final String STEP_MONTHLY = "monthlyProductRankingStep";
    private static final int CHUNK_SIZE = 50;
    private static final int MAX_ITEM_COUNT = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final ProductRankItemProcessor itemProcessor;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Bean(JOB_NAME)
    public Job productRankAggregationJob(
            Step weeklyRankingStep,
            Step monthlyRankingStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(weeklyRankingStep)
                .next(monthlyRankingStep)
                .listener(jobListener)
                .build();
    }

    @Bean(STEP_WEEKLY)
    public Step weeklyRankingStep(
            JpaPagingItemReader<ProductMetrics> productMetricsReader,
            RankingMaterializedViewWriter weeklyRankingWriter
    ) {
        return new StepBuilder(STEP_WEEKLY, jobRepository)
                .<ProductMetrics, ProductRankAggregate>chunk(CHUNK_SIZE, transactionManager)
                .reader(productMetricsReader)
                .processor(itemProcessor)
                .writer(weeklyRankingWriter)
                .listener(stepMonitorListener)
                .build();
    }

    @Bean(STEP_MONTHLY)
    public Step monthlyRankingStep(
            JpaPagingItemReader<ProductMetrics> productMetricsReader,
            RankingMaterializedViewWriter monthlyRankingWriter
    ) {
        return new StepBuilder(STEP_MONTHLY, jobRepository)
                .<ProductMetrics, ProductRankAggregate>chunk(CHUNK_SIZE, transactionManager)
                .reader(productMetricsReader)
                .processor(itemProcessor)
                .writer(monthlyRankingWriter)
                .listener(stepMonitorListener)
                .build();
    }

    @Bean
    @StepScope
    public RankingMaterializedViewWriter weeklyRankingWriter(
            @Value("#{jobParameters['requestDate']}") String requestDate
    ) {
        RankingPeriod period = RankingPeriodResolver.weekly(requestDate);
        return new RankingMaterializedViewWriter(jdbcTemplate, period, "mv_product_rank_weekly");
    }

    @Bean
    @StepScope
    public RankingMaterializedViewWriter monthlyRankingWriter(
            @Value("#{jobParameters['requestDate']}") String requestDate
    ) {
        RankingPeriod period = RankingPeriodResolver.monthly(requestDate);
        return new RankingMaterializedViewWriter(jdbcTemplate, period, "mv_product_rank_monthly");
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<ProductMetrics> productMetricsReader() {
        return new JpaPagingItemReaderBuilder<ProductMetrics>()
                .name("productMetricsReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT p FROM ProductMetrics p ORDER BY (p.likeCount * 0.2d + p.salesCount * 0.8d) DESC"
                )
                .pageSize(CHUNK_SIZE)
                .maxItemCount(MAX_ITEM_COUNT)
                .build();
    }
}
