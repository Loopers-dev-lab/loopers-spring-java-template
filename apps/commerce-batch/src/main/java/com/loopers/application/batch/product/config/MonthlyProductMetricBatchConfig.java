package com.loopers.application.batch.product.config;

import com.loopers.application.batch.product.MonthlyProductMetricBatchPartitioner;
import com.loopers.application.batch.product.MonthlyProductMetricBatchReader;
import com.loopers.application.batch.product.MonthlyProductMetricBatchWriter;
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
public class MonthlyProductMetricBatchConfig {

    @Bean
    public Job monthlyProductMetricJob(
            JobRepository jobRepository,
            Step partitionMonthlyMetricStep
    ) {
        return new JobBuilder("monthlyProductMetricJob", jobRepository)
                .start(partitionMonthlyMetricStep)
                .build();
    }

    @Bean
    public Step partitionMonthlyMetricStep(
            JobRepository jobRepository,
            Step collectMonthlyMetricStep,
            MonthlyProductMetricBatchPartitioner partitioner,
            @Value("${batch.monthly-product-metric.partition.grid-size:4}") int gridSize
    ) {
        return new StepBuilder("partitionMonthlyMetricStep", jobRepository)
                .partitioner("collectMonthlyMetricStep", partitioner)
                .step(collectMonthlyMetricStep)
                .gridSize(gridSize)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<ProductMetricAggregation> synchronizedMonthlyProductMetricReader(
            DailyProductMetricRepository dailyProductMetricRepository
    ) {
        MonthlyProductMetricBatchReader reader = new MonthlyProductMetricBatchReader(dailyProductMetricRepository);
        return new SynchronizedItemStreamReaderBuilder<ProductMetricAggregation>()
                .delegate(reader)
                .build();
    }

    @Bean
    public Step collectMonthlyMetricStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<ProductMetricAggregation> synchronizedMonthlyProductMetricReader,
            MonthlyProductMetricBatchWriter monthlyProductMetricBatchWriter,
            @Value("${batch.monthly-product-metric.chunk:50}") int chunk
    ) {
        return new StepBuilder("collectMonthlyMetricStep", jobRepository)
                .<ProductMetricAggregation, ProductMetricAggregation>chunk(chunk, transactionManager)
                .reader(synchronizedMonthlyProductMetricReader)
                .writer(monthlyProductMetricBatchWriter)
                .taskExecutor(monthlyAsyncTaskExecutor())
                .faultTolerant()
                .retry(DataAccessException.class)
                .build();
    }

    @Bean
    public TaskExecutor monthlyAsyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("monthly-product-metric-batch-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(20);
        return executor;
    }
}
