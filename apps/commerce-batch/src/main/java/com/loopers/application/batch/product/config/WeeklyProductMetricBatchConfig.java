package com.loopers.application.batch.product.config;

import com.loopers.application.batch.product.WeeklyProductMetricBatchPartitioner;
import com.loopers.application.batch.product.WeeklyProductMetricBatchReader;
import com.loopers.application.batch.product.WeeklyProductMetricBatchWriter;
import com.loopers.core.domain.product.repository.DailyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductMetricAggregation;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class WeeklyProductMetricBatchConfig {

    @Bean
    public Job weeklyProductMetricJob(
            JobRepository jobRepository,
            Step partitionDailyMetricStep
    ) {
        return new JobBuilder("weeklyProductMetricJob", jobRepository)
                .start(partitionDailyMetricStep)
                .build();
    }

    @Bean
    public Step partitionDailyMetricStep(
            JobRepository jobRepository,
            Step collectDailyMetricStep,
            WeeklyProductMetricBatchPartitioner partitioner,
            @Value("${batch.weekly-product-metric.partition.grid-size:4}") int gridSize
    ) {
        return new StepBuilder("partitionDailyMetricStep", jobRepository)
                .partitioner("collectDailyMetricStep", partitioner)
                .step(collectDailyMetricStep)
                .taskExecutor(asyncTaskExecutor())
                .gridSize(gridSize)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<ProductMetricAggregation> synchronizedWeeklyProductMetricReader(
            DailyProductMetricRepository dailyProductMetricRepository
    ) {
        WeeklyProductMetricBatchReader reader = new WeeklyProductMetricBatchReader(dailyProductMetricRepository);
        return new SynchronizedItemStreamReaderBuilder<ProductMetricAggregation>()
                .delegate(reader)
                .build();
    }

    @Bean
    public Step collectDailyMetricStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<ProductMetricAggregation> synchronizedWeeklyProductMetricReader,
            WeeklyProductMetricBatchWriter weeklyProductMetricBatchWriter,
            @Value("${batch.weekly-product-metric.chunk:50}") int chunk
    ) {
        return new StepBuilder("collectDailyMetricStep", jobRepository)
                .<ProductMetricAggregation, ProductMetricAggregation>chunk(chunk, transactionManager)
                .reader(synchronizedWeeklyProductMetricReader)
                .writer(weeklyProductMetricBatchWriter)
                .taskExecutor(asyncTaskExecutor())
                .faultTolerant()
                .retry(DataAccessException.class)
                .build();
    }

    @Bean
    public TaskExecutor asyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("weekly-product-metric-batch-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(20);
        return executor;
    }
}
