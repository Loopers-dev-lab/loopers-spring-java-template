package com.loopers.batch.metrics.job;

import com.loopers.batch.listener.ChunkListener;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import com.loopers.batch.metrics.MonthlyMetricsProcessor;
import com.loopers.batch.metrics.MonthlyMetricsWriter;
import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsMonthlyRepository;
import com.loopers.domain.metrics.dto.MonthlyAggregationDto;
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ProductMetricsMonthlyJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductMetricsDailyJpaRepository dailyJpaRepository;
    private final ProductMetricsMonthlyRepository monthlyRepository;

    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final ChunkListener chunkListener;

    @Bean
    public Job productMetricsMonthlyJob() {
        return new JobBuilder("productMetricsMonthlyJob", jobRepository)
                .start(aggregateMonthlyMetricsStep())
                .listener(jobListener)          // Job Listener
                .build();
    }

    @Bean
    public Step aggregateMonthlyMetricsStep() {
        return new StepBuilder("aggregateMonthlyMetricsStep", jobRepository)
                .<MonthlyAggregationDto, ProductMetricsMonthly>chunk(100, transactionManager)
                .reader(monthlyMetricsReader(null, null))
                .processor(monthlyMetricsProcessor())
                .writer(monthlyMetricsWriter())
                .listener(stepMonitorListener)          // Step Listener
                .listener(chunkListener)                // Chunk Listener
                .build();
    }

    /**
     * RepositoryItemReader를 사용하여 DB에서 페이징 집계 수행
     */
    @Bean
    @StepScope
    public RepositoryItemReader<MonthlyAggregationDto> monthlyMetricsReader(
            @Value("#{jobParameters['year']}") Integer year,
            @Value("#{jobParameters['month']}") Integer month
    ) {
        // 월간 시작일/종료일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);  // 말일

        return new RepositoryItemReaderBuilder<MonthlyAggregationDto>()
                .name("monthlyMetricsReader")
                .repository(dailyJpaRepository)
                .methodName("findMonthlyAggregation")
                .arguments(List.of(year, month, startDate, endDate))
                .pageSize(100)  // Chunk Size와 일치
                .sorts(Map.of("productId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<MonthlyAggregationDto, ProductMetricsMonthly> monthlyMetricsProcessor() {
        return new MonthlyMetricsProcessor();
    }

    @Bean
    @StepScope
    public ItemWriter<ProductMetricsMonthly> monthlyMetricsWriter() {
        return new MonthlyMetricsWriter(monthlyRepository);
    }
}
