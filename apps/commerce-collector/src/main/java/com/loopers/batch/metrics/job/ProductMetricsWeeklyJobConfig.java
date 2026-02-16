package com.loopers.batch.metrics.job;

import com.loopers.batch.listener.ChunkListener;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import com.loopers.batch.metrics.WeeklyMetricsProcessor;
import com.loopers.batch.metrics.WeeklyMetricsWriter;
import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.ProductMetricsWeeklyRepository;
import com.loopers.domain.metrics.dto.WeeklyAggregationDto;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ProductMetricsWeeklyJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductMetricsDailyJpaRepository dailyJpaRepository;
    private final ProductMetricsWeeklyRepository weeklyRepository;

    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final ChunkListener chunkListener;

    @Bean
    public Job productMetricsWeeklyJob() {
        return new JobBuilder("productMetricsWeeklyJob", jobRepository)
                .start(aggregateWeeklyMetricsStep())
                .listener(jobListener)          // Job Listener
                .build();
    }

    @Bean
    public Step aggregateWeeklyMetricsStep() {
        return new StepBuilder("aggregateWeeklyMetricsStep", jobRepository)
                .<WeeklyAggregationDto, ProductMetricsWeekly>chunk(100, transactionManager)
                .reader(weeklyMetricsReader(null, null))
                .processor(weeklyMetricsProcessor())
                .writer(weeklyMetricsWriter())
                .listener(stepMonitorListener)          // Step Listener
                .listener(chunkListener)                // Chunk Listener
                .build();
    }

    /**
     * RepositoryItemReader를 사용하여 DB에서 페이징 집계 수행
     */
    @Bean
    @StepScope
    public RepositoryItemReader<WeeklyAggregationDto> weeklyMetricsReader(
            @Value("#{jobParameters['year']}") Integer year,
            @Value("#{jobParameters['week']}") Integer week
    ) {
        // 주간 시작일/종료일 계산
        LocalDate startDate = LocalDate.of(year, 1, 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(DayOfWeek.MONDAY);
        LocalDate endDate = startDate.plusDays(6);

        return new RepositoryItemReaderBuilder<WeeklyAggregationDto>()
                .name("weeklyMetricsReader")
                .repository(dailyJpaRepository)
                .methodName("findWeeklyAggregation")
                .arguments(List.of(year, week, startDate, endDate))
                .pageSize(100)  // Chunk Size와 일치
                .sorts(Map.of("productId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<WeeklyAggregationDto, ProductMetricsWeekly> weeklyMetricsProcessor() {
        return new WeeklyMetricsProcessor();
    }

    @Bean
    @StepScope
    public ItemWriter<ProductMetricsWeekly> weeklyMetricsWriter() {
        return new WeeklyMetricsWriter(weeklyRepository);
    }
}
