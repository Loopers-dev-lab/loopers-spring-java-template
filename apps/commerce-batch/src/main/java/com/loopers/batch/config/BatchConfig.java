package com.loopers.batch.config;

import com.loopers.batch.dto.ProductMetricsAgg;
import com.loopers.batch.dto.ProductRankRow;
import com.loopers.batch.processor.ProductMetricProcessor;
import com.loopers.batch.reader.ProductMetricReader;
import com.loopers.batch.writer.ProductMetricMonthlyWriter;
import com.loopers.batch.writer.ProductMetricWeeklyWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ProductMetricReader productMetricReader;
    private final ProductMetricProcessor productMetricProcessor;
    private final ProductMetricWeeklyWriter productMetricWeeklyWriter;
    private final ProductMetricMonthlyWriter productMetricMonthlyWriter;

    @Bean
    @StepScope
    public JdbcPagingItemReader<ProductMetricsAgg> weeklyProductMetricsAggReader(
            @Value("#{jobParameters['weeklyStartDate']}") String startDate,
            @Value("#{jobParameters['weeklyEndDate']}") String endDate
    ) throws Exception {
        return productMetricReader.reader(startDate, endDate, "weeklyProductMetricsAggReader");
    }

    @Bean
    public Step deleteWeeklyMvStep(@Qualifier("deleteWeeklyMvTasklet") Tasklet deleteWeeklyMvTasklet) {
        return new StepBuilder("deleteWeeklyMvStep", jobRepository)
                .tasklet(deleteWeeklyMvTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet deleteWeeklyMvTasklet(
            @Value("#{jobParameters['weeklyStartDate']}") String startDate,
            @Value("#{jobParameters['weeklyEndDate']}") String endDate
    ) {
        return (contribution, chunkContext) -> {
            String sql = """
            DELETE FROM mv_product_rank_weekly
            WHERE start_date = :weeklyStartDate AND end_date = :weeklyEndDate
            """;
            jdbcTemplate.update(sql, Map.of(
                    "weeklyStartDate", startDate,
                    "weeklyEndDate", endDate
            ));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step aggregateWeeklyRankStep(@Qualifier("weeklyProductMetricsAggReader") JdbcPagingItemReader<ProductMetricsAgg> weeklyProductMetricsAggReader) {
        return new StepBuilder("aggregateWeeklyRankStep", jobRepository)
                .<ProductMetricsAgg, ProductRankRow>chunk(1000, transactionManager)
                .reader(weeklyProductMetricsAggReader)
                .processor(productMetricProcessor)
                .writer(productMetricWeeklyWriter)
                .build();
    }

    @Bean
    public Job weeklyProductRankingJob(@Qualifier("deleteWeeklyMvStep") Step deleteWeeklyMvStep, @Qualifier("aggregateWeeklyRankStep") Step aggregateWeeklyRankStep) {
        return new JobBuilder("weeklyProductRankingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(deleteWeeklyMvStep)
                .next(aggregateWeeklyRankStep)
                .build();
    }


    @Bean
    @StepScope
    public JdbcPagingItemReader<ProductMetricsAgg> monthlyProductMetricsAggReader(
            @Value("#{jobParameters['monthlyStartDate']}") String startDate,
            @Value("#{jobParameters['monthlyEndDate']}") String endDate
    ) throws Exception {
        return productMetricReader.reader(startDate, endDate, "monthlyProductMetricsAggReader");
    }

    @Bean
    public Step deleteMonthlyMvStep(@Qualifier("deleteMonthlyMvTasklet") Tasklet deleteMonthlyMvTasklet) {
        return new StepBuilder("deleteMonthlyMvStep", jobRepository)
                .tasklet(deleteMonthlyMvTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet deleteMonthlyMvTasklet(
            @Value("#{jobParameters['monthlyStartDate']}") String startDate,
            @Value("#{jobParameters['monthlyEndDate']}") String endDate
    ) {
        return (contribution, chunkContext) -> {
            String sql = """
            DELETE FROM mv_product_rank_monthly
            WHERE start_date = :monthlyStartDate AND end_date = :monthlyEndDate
            """;
            jdbcTemplate.update(sql, Map.of(
                    "monthlyStartDate", startDate,
                    "monthlyEndDate", endDate
            ));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step aggregateMonthlyRankStep(@Qualifier("monthlyProductMetricsAggReader") JdbcPagingItemReader<ProductMetricsAgg> monthlyProductMetricsAggReader) {
        return new StepBuilder("aggregateMonthlyRankStep", jobRepository)
                .<ProductMetricsAgg, ProductRankRow>chunk(1000, transactionManager)
                .reader(monthlyProductMetricsAggReader)
                .processor(productMetricProcessor)
                .writer(productMetricMonthlyWriter)
                .build();
    }

    @Bean
    public Job monthlyProductRankingJob(@Qualifier("deleteMonthlyMvStep") Step deleteMonthlyMvStep, @Qualifier("aggregateMonthlyRankStep") Step aggregateMonthlyRankStep) {
        return new JobBuilder("monthlyProductRankingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(deleteMonthlyMvStep)
                .next(aggregateMonthlyRankStep)
                .build();
    }

}
